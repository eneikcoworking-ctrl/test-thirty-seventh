<script>
  import { tick, onMount, onDestroy } from 'svelte';

  // Core App Views
  let currentTab = "inbox"; // "inbox" | "onboard" | "ingest"

  // Auth State
  let isLoggedIn = false;
  let usernameInput = "";
  let passwordInput = "";
  let loginErrorMessage = "";
  let checkingAuth = true;

  // Unified Inbox State
  let chats = [];
  let selectedChatId = null;
  let activeChatMessages = [];
  let newMessageText = "";
  let filterType = "all"; // "all" | "escalated" | "regular" | "closed"
  let searchQuery = "";
  let loadingChats = false;
  let loadingMessages = false;
  let messageContainer;
  let pollInterval = null;

  // Account Onboarding State
  let onboardMode = "otp"; // "otp" | "session"
  let onboardPhone = "";
  let onboardOtp = "";
  let onboardProxyIp = "127.0.0.1";
  let onboardProxyPort = 8080;
  let onboardProxyProtocol = "HTTP";
  let onboardProxyUsername = "";
  let onboardProxyPassword = "";
  let onboardStatus = "idle"; // "idle" | "loading" | "success" | "error"
  let onboardMessage = "";

  // Session File Upload State
  let sessionFile = null;
  let sessionFileName = "";
  let sessionFileSize = "";
  let sessionFileValidationError = "";
  let isDragOver = false;
  let fileInputEl;

  // Lead CSV Ingestion State
  let campaigns = [];
  let selectedCampaignId = "";
  let newCampaignName = "";
  let newCampaignSpintax = "{Hi|Hello|Hey} there! This is customized.";
  let csvContent = "";
  let ingestStatus = "idle"; // "idle" | "loading" | "success" | "error"
  let ingestMessage = "";

  // Reactivity Calculations
  $: activeChat = chats.find(c => c.id === selectedChatId);
  $: escalatedCount = chats.filter(c => c.status === "ESCALATED").length;
  $: activeDealsCount = chats.filter(c => c.status !== "RESOLVED" && c.status !== "CLOSED" && c.status !== "CONVERTED").length;

  $: filteredChats = chats.filter(chat => {
    // Search filter
    const query = searchQuery.toLowerCase().trim();
    const displayName = chat.leadName || chat.leadUsername || "Unknown Lead";
    const matchesSearch = !query ||
      displayName.toLowerCase().includes(query) ||
      (chat.leadUsername && chat.leadUsername.toLowerCase().includes(query)) ||
      (chat.leadPhone && chat.leadPhone.includes(query));

    if (!matchesSearch) return false;

    // Category filter
    if (filterType === "escalated") return chat.status === "ESCALATED";
    if (filterType === "regular") return chat.status === "ACTIVE" || chat.status === "PAUSED";
    if (filterType === "closed") return chat.status === "RESOLVED" || chat.status === "CLOSED" || chat.status === "CONVERTED";
    return true;
  });

  // REST API: Load Conversations
  async function loadConversations(silent = false) {
    if (!silent) loadingChats = true;
    try {
      const res = await fetch("/api/v1/conversations");
      if (res.ok) {
        const data = await res.json();
        // Fallback to empty list if no content
        chats = data.content || [];

        // If there's an active chat, silently refresh its messages
        if (selectedChatId) {
          await loadMessages(selectedChatId, true);
        } else if (chats.length > 0 && !selectedChatId) {
          // Default to the first chat on desktop to avoid empty states
          if (typeof window !== 'undefined' && window.innerWidth >= 768) {
            await selectChat(chats[0].id);
          }
        }
      } else {
        console.error("Failed to load conversations:", res.statusText);
      }
    } catch (err) {
      console.error("Error fetching conversations:", err);
    } finally {
      if (!silent) loadingChats = false;
    }
  }

  // REST API: Load Messages for a specific chat
  async function loadMessages(chatId, silent = false) {
    if (!silent) loadingMessages = true;
    try {
      const res = await fetch(`/api/v1/conversations/${chatId}/messages`);
      if (res.ok) {
        const data = await res.json();
        // Since API returns messages in DESC order (sentAt), let's reverse them for ascending view in bubble stream
        activeChatMessages = [...data].reverse();
        await tick();
        scrollChatToBottom();
      }
    } catch (err) {
      console.error("Error fetching messages:", err);
    } finally {
      if (!silent) loadingMessages = false;
    }
  }

  // Handle selecting a conversation
  async function selectChat(id) {
    selectedChatId = id;
    await loadMessages(id);
  }

  // Scroll active chat stream to bottom
  function scrollChatToBottom() {
    if (messageContainer) {
      messageContainer.scrollTop = messageContainer.scrollHeight;
    }
  }

  // REST API: Send manual representative message
  async function handleSendMessage() {
    if (!newMessageText.trim() || !selectedChatId) return;

    const textToSend = newMessageText.trim();
    newMessageText = ""; // Optimistic clearing of input

    try {
      const res = await fetch(`/api/v1/conversations/${selectedChatId}/messages`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ text: textToSend })
      });

      if (res.ok) {
        const sentMessage = await res.json();
        // Append sent message to local chat stream for instant feedback
        activeChatMessages = [...activeChatMessages, sentMessage];
        // Trigger a conversation list refresh to update the status to PAUSED and the timestamp
        await loadConversations(true);
        await tick();
        scrollChatToBottom();
      } else {
        console.error("Failed to send message:", res.statusText);
      }
    } catch (err) {
      console.error("Error sending message:", err);
    }
  }

  // Keyboard handling in compose box
  function handleKeydown(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      handleSendMessage();
    }
  }

  // REST API: Onboard Account via OTP
  async function submitOnboarding() {
    if (!onboardPhone || !onboardOtp || !onboardProxyIp) {
      onboardStatus = "error";
      onboardMessage = "Phone, OTP code, and Proxy IP address are required.";
      return;
    }

    onboardStatus = "loading";
    onboardMessage = "";

    try {
      const res = await fetch("/api/accounts/onboard/otp", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          phoneNumber: onboardPhone,
          otpCode: onboardOtp,
          proxyIp: onboardProxyIp,
          proxyPort: onboardProxyPort,
          proxyProtocol: onboardProxyProtocol,
          proxyUsername: onboardProxyUsername,
          proxyPassword: onboardProxyPassword
        })
      });

      const data = await res.json();
      if (res.ok || res.status === 201) {
        onboardStatus = "success";
        onboardMessage = data.message || "Account successfully onboarded!";
        // Clear onboarding form inputs
        onboardPhone = "";
        onboardOtp = "";
      } else {
        onboardStatus = "error";
        onboardMessage = data.error || "Onboarding failed. Please check your OTP and proxy configurations.";
      }
    } catch (err) {
      onboardStatus = "error";
      onboardMessage = "Error connecting to onboarding service: " + err.message;
    }
  }

  // Session file onboarding handlers & validation
  function validateAndSetFile(file) {
    sessionFileValidationError = "";
    if (!file) return;

    // Check extension: must be .session or .tdata (case-insensitive)
    const ext = file.name.split('.').pop().toLowerCase();
    if (ext !== 'session' && ext !== 'tdata') {
      sessionFileValidationError = "Invalid file format. Only .session or .tdata files are allowed.";
      sessionFile = null;
      sessionFileName = "";
      sessionFileSize = "";
      return;
    }

    // Limit check for size (large files)
    if (file.size > 10 * 1024 * 1024) { // 10MB
      sessionFileValidationError = "File size exceeds limit. Please upload a valid, smaller session file.";
      sessionFile = null;
      sessionFileName = "";
      sessionFileSize = "";
      return;
    }

    sessionFile = file;
    sessionFileName = file.name;
    sessionFileSize = (file.size / 1024).toFixed(1) + " KB";
  }

  function handleFileSelect(e) {
    const file = e.target.files[0];
    validateAndSetFile(file);
  }

  function handleFileDrop(e) {
    isDragOver = false;
    const file = e.dataTransfer.files[0];
    validateAndSetFile(file);
  }

  function removeSessionFile() {
    sessionFile = null;
    sessionFileName = "";
    sessionFileSize = "";
    sessionFileValidationError = "";
  }

  function handleKeyPress(e) {
    if ((e.key === 'Enter' || e.key === ' ') && fileInputEl) {
      e.preventDefault();
      fileInputEl.click();
    }
  }

  async function submitSessionOnboarding() {
    if (!onboardPhone) {
      onboardStatus = "error";
      onboardMessage = "Phone number is required.";
      return;
    }

    if (!sessionFile) {
      onboardStatus = "error";
      onboardMessage = "Please select or drag-and-drop a valid .session or .tdata file first.";
      return;
    }

    if (!onboardProxyIp) {
      onboardStatus = "error";
      onboardMessage = "Proxy IP address is required.";
      return;
    }

    onboardStatus = "loading";
    onboardMessage = "";

    const formData = new FormData();
    formData.append("phoneNumber", onboardPhone);
    formData.append("sessionFile", sessionFile);
    formData.append("proxyIp", onboardProxyIp);
    formData.append("proxyPort", onboardProxyPort);
    formData.append("proxyProtocol", onboardProxyProtocol);
    if (onboardProxyUsername) {
      formData.append("proxyUsername", onboardProxyUsername);
    }
    if (onboardProxyPassword) {
      formData.append("proxyPassword", onboardProxyPassword);
    }

    // Setup network timeout (AbortController) to handle large file sizes or slow network
    const controller = new AbortController();
    const timeoutMs = sessionFile.size > 200 * 1024 ? 50 : 15000;
    const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

    try {
      const res = await fetch("/api/accounts/onboard/session", {
        method: "POST",
        body: formData,
        signal: controller.signal
      });
      clearTimeout(timeoutId);

      const data = await res.json();
      if (res.ok || res.status === 201) {
        onboardStatus = "success";
        const activeStatus = data.status || "Active";
        onboardMessage = `${data.message || "Account successfully registered from session file!"} (Status: ${activeStatus})`;
        onboardPhone = "";
        removeSessionFile();
      } else {
        onboardStatus = "error";
        onboardMessage = data.error || "Onboarding failed. Please check your session file and proxy configurations.";
      }
    } catch (err) {
      clearTimeout(timeoutId);
      if (err.name === 'AbortError') {
        onboardStatus = "error";
        onboardMessage = "Upload timed out. The session file size might be too large or the connection is too slow.";
      } else {
        onboardStatus = "error";
        onboardMessage = "Error connecting to onboarding service: " + err.message;
      }
    }
  }

  // REST API: Load campaigns for lead ingestion
  async function loadCampaigns() {
    try {
      const res = await fetch("/api/v1/campaigns");
      if (res.ok) {
        campaigns = await res.json();
        if (campaigns.length > 0 && !selectedCampaignId) {
          selectedCampaignId = campaigns[0].id;
        }
      }
    } catch (err) {
      console.error("Error loading campaigns:", err);
    }
  }

  // REST API: Create Campaign and/or Import Leads
  async function submitIngestLeads() {
    ingestStatus = "loading";
    ingestMessage = "";

    try {
      let campaignId = selectedCampaignId;

      // 1. Create a campaign first if "New Campaign Name" is filled
      if (newCampaignName.trim()) {
        const createRes = await fetch("/api/v1/campaigns", {
          method: "POST",
          headers: {
            "Content-Type": "application/json"
          },
          body: JSON.stringify({
            name: newCampaignName.trim(),
            spintaxRules: newCampaignSpintax.trim()
          })
        });

        if (createRes.ok) {
          const newCampaign = await createRes.json();
          campaignId = newCampaign.id;
          newCampaignName = ""; // Clear
          await loadCampaigns(); // Refresh list
        } else {
          const errData = await createRes.json();
          ingestStatus = "error";
          ingestMessage = "Failed to create new campaign: " + (errData.message || createRes.statusText);
          return;
        }
      }

      // Check if we have a campaign ID to target
      if (!campaignId) {
        ingestStatus = "error";
        ingestMessage = "Please select or create an outreach campaign first.";
        return;
      }

      if (!csvContent.trim()) {
        ingestStatus = "error";
        ingestMessage = "CSV leads content must not be empty.";
        return;
      }

      // 2. Perform ingestion endpoint call
      const ingestRes = await fetch(`/api/v1/campaigns/${campaignId}/leads/import`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          content: csvContent.trim()
        })
      });

      if (ingestRes.ok) {
        const importedLeads = await ingestRes.json();
        ingestStatus = "success";
        ingestMessage = `Successfully imported ${importedLeads.length} leads into the campaign!`;
        csvContent = ""; // Clear text area
      } else {
        const errData = await ingestRes.json();
        ingestStatus = "error";
        ingestMessage = "Import failed: " + (errData.message || ingestRes.statusText);
      }
    } catch (err) {
      ingestStatus = "error";
      ingestMessage = "Error during lead import process: " + err.message;
    }
  }

  // Auth: Check login status
  async function checkAuthStatus() {
    try {
      const res = await fetch("/api/v1/auth/status");
      if (res.ok) {
        isLoggedIn = true;
        loadConversations();
        loadCampaigns();
      } else {
        isLoggedIn = false;
      }
    } catch (err) {
      isLoggedIn = false;
    } finally {
      checkingAuth = false;
    }
  }

  // Auth: Log in
  async function handleLogin() {
    loginErrorMessage = "";
    if (!usernameInput.trim() || !passwordInput.trim()) {
      loginErrorMessage = "Username and password are required.";
      return;
    }
    try {
      const res = await fetch("/api/v1/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username: usernameInput.trim(), password: passwordInput.trim() })
      });
      if (res.ok) {
        isLoggedIn = true;
        loginErrorMessage = "";
        usernameInput = "";
        passwordInput = "";
        loadConversations();
        loadCampaigns();
      } else {
        const data = await res.json().catch(() => ({}));
        loginErrorMessage = data.message || "Invalid username or password.";
      }
    } catch (err) {
      loginErrorMessage = "Failed to connect to the server.";
    }
  }

  // Auth: Log out
  async function handleLogout() {
    try {
      await fetch("/api/v1/auth/logout", { method: "POST" });
    } catch (err) {
      // ignore
    } finally {
      isLoggedIn = false;
    }
  }

  // Lifecycle & Real-time Sync
  onMount(() => {
    // Intercept 401 Unauthorized responses to force login screen
    const originalFetch = window.fetch;
    window.fetch = async (...args) => {
      const response = await originalFetch(...args);
      if (response.status === 401 && !args[0].includes('/api/v1/auth/login')) {
        isLoggedIn = false;
      }
      return response;
    };

    checkAuthStatus();

    // Start 5s polling interval to fetch live messages & conversation lists in real time
    pollInterval = setInterval(() => {
      if (isLoggedIn) {
        loadConversations(true);
      }
    }, 5000);
  });

  onDestroy(() => {
    if (pollInterval) {
      clearInterval(pollInterval);
    }
  });
</script>

<div class="min-h-screen bg-slate-50 text-slate-900 flex flex-col font-sans">
  {#if checkingAuth}
    <!-- Loading spinner page -->
    <div class="flex-1 flex flex-col items-center justify-center p-8">
      <div class="animate-spin h-10 w-10 border-4 border-[#003ec7] border-t-transparent rounded-full mb-4"></div>
      <p class="text-sm text-slate-600 font-semibold">Initializing LeadGen Bot Control Center...</p>
    </div>
  {:else if !isLoggedIn}
    <!-- Centered Login Card Panel -->
    <div class="flex-1 flex items-center justify-center p-4 bg-slate-100">
      <div class="w-full max-w-md bg-white border border-slate-200 rounded-2xl shadow-xl overflow-hidden">
        <header class="bg-[#003ec7] text-white p-6 text-center">
          <span class="material-symbols-outlined text-[48px]" aria-hidden="true">admin_panel_settings</span>
          <h2 class="font-bold text-xl md:text-2xl tracking-wide mt-2">Admin Panel Login</h2>
          <p class="text-xs text-blue-100 mt-1">LeadGen Bot Live Chat CRM</p>
        </header>

        <form on:submit|preventDefault={handleLogin} class="p-6 space-y-4">
          {#if loginErrorMessage}
            <div class="p-3 bg-red-50 border border-red-200 text-red-800 rounded-xl text-xs flex gap-2 items-start" role="alert">
              <span class="material-symbols-outlined text-red-600 text-[18px]">error</span>
              <p class="font-semibold">{loginErrorMessage}</p>
            </div>
          {/if}

          <div class="flex flex-col gap-1.5">
            <label for="login-username" class="text-xs font-bold text-slate-700 uppercase tracking-wider">Username</label>
            <input
              id="login-username"
              type="text"
              placeholder="Enter username"
              bind:value={usernameInput}
              class="w-full px-3 py-2 text-sm border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#003ec7]"
              required
            />
          </div>

          <div class="flex flex-col gap-1.5">
            <label for="login-password" class="text-xs font-bold text-slate-700 uppercase tracking-wider">Password</label>
            <input
              id="login-password"
              type="password"
              placeholder="Enter password"
              bind:value={passwordInput}
              class="w-full px-3 py-2 text-sm border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#003ec7]"
              required
            />
          </div>

          <button
            type="submit"
            class="w-full py-2.5 bg-[#003ec7] hover:bg-blue-800 text-white font-bold rounded-xl shadow-md transition-all flex items-center justify-center gap-2 mt-2"
          >
            <span class="material-symbols-outlined">login</span>
            Sign In
          </button>
        </form>
      </div>
    </div>
  {:else}
    <!-- Header Application Shell (Matches design colors/styles) -->
    <header class="bg-[#003ec7] text-white h-16 px-4 md:px-6 flex items-center justify-between shadow-md z-10 flex-shrink-0">
      <div class="flex items-center gap-3">
        <span class="material-symbols-outlined text-[28px]" aria-hidden="true">forum</span>
        <h1 class="font-bold text-lg md:text-xl tracking-wide">LeadGen Bot Control Center</h1>
      </div>

      <!-- Navigation Tabs for CRM Operators -->
      <nav class="hidden md:flex items-center gap-2 bg-[#002cb3] p-1 rounded-xl" aria-label="Main system modules">
        <button
          class="px-4 py-1.5 text-xs font-semibold rounded-lg transition-all flex items-center gap-1.5 {currentTab === 'inbox' ? 'bg-white text-[#003ec7] shadow' : 'text-blue-100 hover:text-white'}"
          on:click={() => currentTab = "inbox"}
        >
          <span class="material-symbols-outlined text-[16px]">chat</span>
          Unified Inbox
        </button>
        <button
          class="px-4 py-1.5 text-xs font-semibold rounded-lg transition-all flex items-center gap-1.5 {currentTab === 'onboard' ? 'bg-white text-[#003ec7] shadow' : 'text-blue-100 hover:text-white'}"
          on:click={() => currentTab = "onboard"}
        >
          <span class="material-symbols-outlined text-[16px]">key</span>
          Account Onboarding
        </button>
        <button
          class="px-4 py-1.5 text-xs font-semibold rounded-lg transition-all flex items-center gap-1.5 {currentTab === 'ingest' ? 'bg-white text-[#003ec7] shadow' : 'text-blue-100 hover:text-white'}"
          on:click={() => currentTab = "ingest"}
        >
          <span class="material-symbols-outlined text-[16px]">upload_file</span>
          Lead Ingestion
        </button>
      </nav>

      <div class="flex items-center gap-4">
        <div class="hidden lg:flex items-center gap-3 text-xs font-semibold">
          <span class="bg-yellow-400 text-slate-950 px-2.5 py-1 rounded-full flex items-center gap-1 shadow-sm">
            <span class="material-symbols-outlined text-[14px]" style="font-variation-settings: 'FILL' 1;">warning</span>
            {escalatedCount} ESCALATED
          </span>
          <span class="bg-blue-600 text-white border border-blue-400 px-2.5 py-1 rounded-full flex items-center gap-1 shadow-sm">
            <span class="material-symbols-outlined text-[14px]">handshake</span>
            {activeDealsCount} ACTIVE DEALS
          </span>
        </div>
        <div class="flex items-center gap-2">
          <span class="text-xs text-blue-100 hidden sm:inline">Admin</span>
          <button
            on:click={handleLogout}
            class="text-xs text-blue-100 hover:text-white underline font-semibold flex items-center gap-1 focus:outline-none"
          >
            <span class="material-symbols-outlined text-[14px]">logout</span>
            Logout
          </button>
        </div>
      </div>
    </header>

    <!-- Navigation Bar (Mobile Only) -->
    <nav class="md:hidden bg-white border-b border-slate-200 flex justify-around py-2.5 shadow-sm" aria-label="Mobile Navigation">
      <button
        class="flex flex-col items-center gap-1 text-[11px] font-bold {currentTab === 'inbox' ? 'text-[#003ec7]' : 'text-slate-500'}"
        on:click={() => currentTab = "inbox"}
      >
        <span class="material-symbols-outlined text-[20px]">chat</span>
        Inbox
      </button>
      <button
        class="flex flex-col items-center gap-1 text-[11px] font-bold {currentTab === 'onboard' ? 'text-[#003ec7]' : 'text-slate-500'}"
        on:click={() => currentTab = "onboard"}
      >
        <span class="material-symbols-outlined text-[20px]">key</span>
        Onboarding
      </button>
      <button
        class="flex flex-col items-center gap-1 text-[11px] font-bold {currentTab === 'ingest' ? 'text-[#003ec7]' : 'text-slate-500'}"
        on:click={() => currentTab = "ingest"}
      >
        <span class="material-symbols-outlined text-[20px]">upload_file</span>
        Lead Ingestion
      </button>
    </nav>

    <!-- Responsive Content Body Container -->
    <div class="flex-1 flex overflow-hidden">
      <!-- VIEW 1: UNIFIED INBOX MODULE -->
      {#if currentTab === 'inbox'}
        <!-- Chat List Sidebar (Hides on mobile when a chat is open) -->
        <main class="w-full md:w-[380px] lg:w-[420px] flex flex-col border-r border-slate-200 bg-white {selectedChatId && 'hidden md:flex'}">
          <!-- Search and Filter Bar -->
          <div class="p-3 border-b border-slate-200 space-y-2 bg-white">
            <div class="relative">
              <span class="material-symbols-outlined absolute left-3 top-2.5 text-slate-400 text-[20px]" aria-hidden="true">search</span>
              <input
                type="text"
                placeholder="Search leads..."
                bind:value={searchQuery}
                class="w-full pl-9 pr-3 py-2 text-sm bg-slate-50 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#003ec7] focus:bg-white transition-all"
                aria-label="Search conversations"
              />
            </div>

            <!-- Filter categories -->
            <div class="flex bg-slate-100 p-0.5 rounded-lg text-xs" role="tablist" aria-label="Conversation filters">
              <button
                role="tab"
                aria-selected={filterType === 'all'}
                class="flex-1 py-1.5 font-semibold text-center rounded-md transition-all {filterType === 'all' ? 'bg-white text-[#003ec7] shadow-sm' : 'text-slate-600 hover:text-slate-900'}"
                on:click={() => filterType = "all"}
              >
                All
              </button>
              <button
                role="tab"
                aria-selected={filterType === 'escalated'}
                class="flex-1 py-1.5 font-semibold text-center rounded-md transition-all flex items-center justify-center gap-1 {filterType === 'escalated' ? 'bg-yellow-400 text-slate-950 shadow-sm' : 'text-slate-600 hover:text-slate-900'}"
                on:click={() => filterType = "escalated"}
              >
                Escalated
              </button>
              <button
                role="tab"
                aria-selected={filterType === 'regular'}
                class="flex-1 py-1.5 font-semibold text-center rounded-md transition-all {filterType === 'regular' ? 'bg-white text-[#003ec7] shadow-sm' : 'text-slate-600 hover:text-slate-900'}"
                on:click={() => filterType = "regular"}
              >
                Regular
              </button>
              <button
                role="tab"
                aria-selected={filterType === 'closed'}
                class="flex-1 py-1.5 font-semibold text-center rounded-md transition-all {filterType === 'closed' ? 'bg-white text-[#003ec7] shadow-sm' : 'text-slate-600 hover:text-slate-900'}"
                on:click={() => filterType = "closed"}
              >
                Closed
              </button>
            </div>
          </div>

          <!-- Conversations Stream/List -->
          <section class="flex-1 overflow-y-auto divide-y divide-slate-100" aria-label="Conversation list">
            {#if loadingChats && chats.length === 0}
              <div class="p-8 text-center text-slate-400 text-sm">
                <p>Loading conversations from CRM API...</p>
              </div>
            {:else if filteredChats.length === 0}
              <div class="p-8 text-center text-slate-400 text-sm">
                <span class="material-symbols-outlined text-[36px] mb-1" aria-hidden="true">chat_bubble_outline</span>
                <p>No conversations found</p>
              </div>
            {:else}
              {#each filteredChats as chat (chat.id)}
                <button
                  on:click={() => selectChat(chat.id)}
                  class="w-full text-left p-4 flex gap-3 transition-all relative outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-[#003ec7] z-0
                    {chat.id === selectedChatId ? 'bg-blue-50/50' : 'hover:bg-slate-50'}
                    {chat.status === 'ESCALATED' ? 'bg-yellow-50/95 border-l-4 border-yellow-500' : 'border-l-4 border-transparent'}"
                  aria-label="Chat with {chat.leadName || chat.leadUsername}. Status: {chat.status}"
                >
                  <!-- Avatar placeholder with initial -->
                  <div class="relative flex-shrink-0">
                    <div class="w-12 h-12 rounded-full bg-blue-100 border border-blue-200 flex items-center justify-center font-bold text-lg text-blue-800">
                      {(chat.leadName || chat.leadUsername || "U").charAt(0).toUpperCase()}
                    </div>
                    {#if chat.status === 'ESCALATED'}
                      <div class="absolute -bottom-1 -right-1 bg-yellow-500 text-slate-950 rounded-full p-0.5 border-2 border-white flex items-center justify-center" aria-hidden="true">
                        <span class="material-symbols-outlined text-[13px] font-bold">priority_high</span>
                      </div>
                    {/if}
                  </div>

                  <!-- Message Details -->
                  <div class="flex-1 min-w-0">
                    <div class="flex justify-between items-baseline mb-1">
                      <h2 class="font-semibold text-sm text-slate-900 truncate">{chat.leadName || chat.leadUsername || "Anonymous lead"}</h2>
                      <span class="text-xs text-slate-500 flex-shrink-0">
                        {chat.lastMessageAt ? new Date(chat.lastMessageAt).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'}) : ""}
                      </span>
                    </div>

                    <div class="flex items-center gap-2 mb-1.5 flex-wrap">
                      <span class="text-xs font-medium text-slate-500">@{chat.leadUsername || "no_username"}</span>
                      {#if chat.status === 'ACTIVE'}
                        <span class="bg-green-100 text-green-800 border border-green-300 font-bold px-1.5 py-0.5 rounded text-[9px] uppercase tracking-wider">
                          AI Active
                        </span>
                      {:else if chat.status === 'ESCALATED'}
                        <span class="bg-yellow-100 text-yellow-800 border border-yellow-300 font-bold px-1.5 py-0.5 rounded text-[9px] uppercase tracking-wider flex items-center gap-0.5">
                          <span class="material-symbols-outlined text-[10px]">warning</span>
                          Human Intervention Required
                        </span>
                      {:else if chat.status === 'PAUSED'}
                        <span class="bg-blue-50 text-blue-600 border border-blue-200 font-bold px-1.5 py-0.5 rounded text-[9px] uppercase tracking-wider">
                          Paused AI
                        </span>
                      {:else if chat.status === 'RESOLVED' || chat.status === 'CLOSED' || chat.status === 'CONVERTED'}
                        <span class="bg-blue-100 text-blue-800 border border-blue-300 font-bold px-1.5 py-0.5 rounded text-[9px] uppercase tracking-wider">
                          Closed/Converted
                        </span>
                      {:else if chat.status === 'SPAM' || chat.status === 'BLOCKED'}
                        <span class="bg-red-100 text-red-800 border border-red-300 font-bold px-1.5 py-0.5 rounded text-[9px] uppercase tracking-wider">
                          Spam/Blocked
                        </span>
                      {:else}
                        <span class="bg-slate-100 text-slate-800 border border-slate-300 font-semibold px-1.5 py-0.5 rounded text-[9px] uppercase tracking-wider">
                          {chat.status}
                        </span>
                      {/if}
                    </div>
                  </div>
                </button>
              {/each}
            {/if}
          </section>
        </main>

        <!-- Chat Window Panel -->
        <section class="flex-1 flex flex-col bg-slate-100 {!selectedChatId && 'hidden md:flex'} {selectedChatId ? 'flex' : 'hidden'}">
          {#if activeChat}
            <!-- Active Chat Header -->
            <header class="bg-white border-b border-slate-200 h-16 px-4 flex items-center justify-between shadow-sm flex-shrink-0">
              <div class="flex items-center gap-3">
                <button
                  on:click={() => selectedChatId = null}
                  class="md:hidden p-2 -ml-2 rounded-full hover:bg-slate-100 text-slate-600 focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-[#003ec7] outline-none"
                  aria-label="Back to chat list"
                >
                  <span class="material-symbols-outlined text-[24px]">arrow_back</span>
                </button>

                <div class="w-10 h-10 rounded-full bg-blue-100 border border-blue-200 flex items-center justify-center font-bold text-blue-800">
                  {(activeChat.leadName || activeChat.leadUsername || "U").charAt(0).toUpperCase()}
                </div>
                <div>
                  <div class="flex items-center gap-2 flex-wrap">
                    <h2 class="font-bold text-sm text-slate-900">{activeChat.leadName || activeChat.leadUsername || "Lead"}</h2>
                    {#if activeChat.status === 'ACTIVE'}
                      <span class="bg-green-100 text-green-800 border border-green-300 px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider flex items-center gap-0.5">
                        <span class="material-symbols-outlined text-[11px]">auto_awesome</span>
                        AI Active
                      </span>
                    {:else if activeChat.status === 'ESCALATED'}
                      <span class="bg-yellow-100 text-yellow-800 border border-yellow-300 px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider flex items-center gap-0.5 animate-pulse">
                        <span class="material-symbols-outlined text-[11px]">warning</span>
                        Human Intervention Required
                      </span>
                    {:else if activeChat.status === 'PAUSED'}
                      <span class="bg-blue-50 text-blue-600 border border-blue-200 px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider">
                        Paused AI
                      </span>
                    {:else if activeChat.status === 'RESOLVED' || activeChat.status === 'CLOSED' || activeChat.status === 'CONVERTED'}
                      <span class="bg-blue-100 text-blue-800 border border-blue-300 px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider">
                        Closed/Converted
                      </span>
                    {:else if activeChat.status === 'SPAM' || activeChat.status === 'BLOCKED'}
                      <span class="bg-red-100 text-red-800 border border-red-300 px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider">
                        Spam/Blocked
                      </span>
                    {:else}
                      <span class="bg-slate-100 text-slate-800 border border-slate-300 px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider">
                        {activeChat.status}
                      </span>
                    {/if}
                  </div>
                  <p class="text-xs text-slate-500 truncate max-w-[200px] sm:max-w-md">@{activeChat.leadUsername || "no_username"} • {activeChat.leadPhone || "No Phone"}</p>
                </div>
              </div>
            </header>

            <!-- Message Stream Area -->
            <div
              bind:this={messageContainer}
              class="flex-1 overflow-y-auto p-4 md:p-6 space-y-4"
              role="log"
              aria-live="polite"
              aria-label="Message stream"
            >
              {#if loadingMessages && activeChatMessages.length === 0}
                <p class="text-center text-slate-400 text-sm">Loading message history...</p>
              {:else}
                <!-- Announcement banner for escalated status -->
                {#if activeChat.status === 'ESCALATED'}
                  <div class="bg-yellow-50 border border-yellow-200 rounded-xl p-3 text-center max-w-lg mx-auto shadow-sm">
                    <div class="flex items-center justify-center gap-1.5 text-yellow-800 font-bold text-xs uppercase tracking-wider mb-1">
                      <span class="material-symbols-outlined text-[16px]">auto_awesome</span>
                      AI Agent Handoff Required
                    </div>
                    <p class="text-xs text-yellow-900 leading-relaxed">
                      This user has triggered safety keywords or explicitly requested human operators. Send a message manually below to paused AI replies.
                    </p>
                  </div>
                {/if}

                <!-- Message bubbles -->
                {#each activeChatMessages as message (message.id)}
                  <div class="flex {message.senderType === 'HUMAN_REPRESENTATIVE' ? 'justify-end' : 'justify-start'}">
                    <div class="max-w-[75%] sm:max-w-[65%] rounded-2xl px-4 py-2.5 shadow-sm text-sm
                      {message.senderType === 'HUMAN_REPRESENTATIVE'
                        ? 'bg-[#003ec7] text-white rounded-tr-none'
                        : message.senderType === 'AI_AGENT'
                          ? 'bg-amber-50 border border-amber-200 text-amber-950 rounded-tl-none relative before:content-[\'🤖_AI\'] before:block before:text-[9px] before:font-bold before:text-amber-700 before:mb-1'
                          : 'bg-white text-slate-800 border border-slate-200 rounded-tl-none'}"
                    >
                      <p class="leading-relaxed break-words">{message.text}</p>
                      <span class="block text-[10px] mt-1 text-right {message.senderType === 'HUMAN_REPRESENTATIVE' ? 'text-blue-200' : 'text-slate-400'}">
                        {new Date(message.sentAt).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}
                      </span>
                    </div>
                  </div>
                {/each}
              {/if}
            </div>

            <!-- Composer Area -->
            <footer class="bg-white border-t border-slate-200 p-3 md:p-4 flex-shrink-0">
              <div class="flex items-end gap-2 max-w-4xl mx-auto">
                <div class="flex-1 relative">
                  <textarea
                    rows="1"
                    placeholder="Type your message to {activeChat.leadName || activeChat.leadUsername}..."
                    bind:value={newMessageText}
                    on:keydown={handleKeydown}
                    class="w-full bg-slate-50 border border-slate-200 focus:border-[#003ec7] focus:ring-2 focus:ring-[#003ec7] focus:bg-white rounded-xl py-2.5 pl-4 pr-10 text-sm resize-none outline-none transition-all max-h-32"
                    aria-label="Type message"
                  ></textarea>
                </div>

                <button
                  on:click={handleSendMessage}
                  disabled={!newMessageText.trim()}
                  class="bg-[#003ec7] hover:bg-blue-800 disabled:opacity-40 disabled:hover:bg-[#003ec7] text-white font-bold p-2.5 rounded-xl shadow-md transition-all flex items-center justify-center focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-[#003ec7] outline-none active:scale-95 flex-shrink-0"
                  aria-label="Send message"
                >
                  <span class="material-symbols-outlined">send</span>
                </button>
              </div>
            </footer>
          {:else}
            <!-- Empty State (No chat selected, only possible on desktop layout) -->
            <div class="flex-1 flex flex-col items-center justify-center p-8 text-center text-slate-400">
              <span class="material-symbols-outlined text-[64px] mb-3 text-slate-300">forum_outline</span>
              <h2 class="font-bold text-slate-600 text-lg mb-1">Unified Inbox</h2>
              <p class="text-sm max-w-sm">Select an active or escalated conversation from the sidebar list to view history and chat.</p>
            </div>
          {/if}
        </section>
      {/if}

      <!-- VIEW 2: ACCOUNT ONBOARDING MODULE -->
      {#if currentTab === 'onboard'}
        <main class="flex-1 overflow-y-auto p-4 md:p-8 bg-slate-50">
          <div class="max-w-2xl mx-auto bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
            <header class="bg-[#003ec7] text-white p-6">
              <div class="flex items-center gap-3">
                <span class="material-symbols-outlined text-[28px]">key</span>
                <h2 class="font-bold text-lg md:text-xl">Onboard Telegram Account</h2>
              </div>
              <p class="text-xs text-blue-100 mt-1">Authenticate a new worker/agent session using OTP or by uploading pre-authenticated session files.</p>
            </header>

            <!-- Sub-navigation: OTP vs Session File -->
            <div class="flex border-b border-slate-200 bg-slate-50/50">
              <button
                type="button"
                class="flex-1 py-3 text-sm font-semibold text-center border-b-2 transition-all {onboardMode === 'otp' ? 'border-[#003ec7] text-[#003ec7] bg-white' : 'border-transparent text-slate-500 hover:text-slate-800'}"
                on:click={() => { onboardMode = 'otp'; onboardStatus = 'idle'; onboardMessage = ''; }}
              >
                OTP Authentication
              </button>
              <button
                type="button"
                class="flex-1 py-3 text-sm font-semibold text-center border-b-2 transition-all {onboardMode === 'session' ? 'border-[#003ec7] text-[#003ec7] bg-white' : 'border-transparent text-slate-500 hover:text-slate-800'}"
                on:click={() => { onboardMode = 'session'; onboardStatus = 'idle'; onboardMessage = ''; }}
              >
                Session File Upload
              </button>
            </div>

            {#if onboardMode === 'otp'}
              <form on:submit|preventDefault={submitOnboarding} class="p-6 space-y-6">
                {#if onboardStatus === "success"}
                  <div class="p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl text-sm flex gap-2 items-start" role="alert">
                    <span class="material-symbols-outlined text-emerald-600">check_circle</span>
                    <div>
                      <p class="font-bold">Successfully Registered!</p>
                      <p class="text-xs mt-0.5">{onboardMessage}</p>
                    </div>
                  </div>
                {:else if onboardStatus === "error"}
                  <div class="p-4 bg-red-50 border border-red-200 text-red-800 rounded-xl text-sm flex gap-2 items-start" role="alert">
                    <span class="material-symbols-outlined text-red-600">error</span>
                    <div>
                      <p class="font-bold">Onboarding Failed</p>
                      <p class="text-xs mt-0.5">{onboardMessage}</p>
                    </div>
                  </div>
                {/if}

                <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <!-- Phone -->
                  <div class="flex flex-col gap-1.5">
                    <label for="onboard-phone" class="text-xs font-bold text-slate-700 uppercase tracking-wider">Phone Number</label>
                    <input
                      id="onboard-phone"
                      type="text"
                      placeholder="+1234567890"
                      bind:value={onboardPhone}
                      class="w-full px-3 py-2 text-sm border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#003ec7]"
                      required
                    />
                  </div>

                  <!-- OTP Code -->
                  <div class="flex flex-col gap-1.5">
                    <label for="onboard-otp" class="text-xs font-bold text-slate-700 uppercase tracking-wider">OTP Code</label>
                    <input
                      id="onboard-otp"
                      type="text"
                      placeholder="12345"
                      bind:value={onboardOtp}
                      class="w-full px-3 py-2 text-sm border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#003ec7]"
                      required
                    />
                  </div>
                </div>

                <!-- Proxy Settings Card -->
                <div class="border border-slate-100 bg-slate-50 p-4 rounded-xl space-y-4">
                  <h3 class="text-xs font-bold text-slate-700 uppercase tracking-wider flex items-center gap-1.5">
                    <span class="material-symbols-outlined text-[16px]">settings_ethernet</span>
                    Proxy Configuration
                  </h3>

                  <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <div class="flex flex-col gap-1.5">
                      <label for="onboard-proxy-ip" class="text-[10px] font-bold text-slate-500 uppercase">Proxy IP</label>
                      <input
                        id="onboard-proxy-ip"
                        type="text"
                        bind:value={onboardProxyIp}
                        class="w-full px-3 py-1.5 text-sm bg-white border border-slate-200 rounded-lg focus:ring-2 focus:ring-[#003ec7]"
                        required
                      />
                    </div>

                    <div class="flex flex-col gap-1.5">
                      <label for="onboard-proxy-port" class="text-[10px] font-bold text-slate-500 uppercase">Port</label>
                      <input
                        id="onboard-proxy-port"
                        type="number"
                        bind:value={onboardProxyPort}
                        class="w-full px-3 py-1.5 text-sm bg-white border border-slate-200 rounded-lg focus:ring-2 focus:ring-[#003ec7]"
                        required
                      />
                    </div>

                    <div class="flex flex-col gap-1.5">
                      <label for="onboard-proxy-protocol" class="text-[10px] font-bold text-slate-500 uppercase">Protocol</label>
                      <select
                        id="onboard-proxy-protocol"
                        bind:value={onboardProxyProtocol}
                        class="w-full px-3 py-1.5 text-sm bg-white border border-slate-200 rounded-lg focus:ring-2 focus:ring-[#003ec7]"
                      >
                        <option value="HTTP">HTTP</option>
                        <option value="SOCKS5">SOCKS5</option>
                      </select>
                    </div>
                  </div>

                  <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div class="flex flex-col gap-1.5">
                      <label for="onboard-proxy-user" class="text-[10px] font-bold text-slate-500 uppercase">Proxy Username (Optional)</label>
                      <input
                        id="onboard-proxy-user"
                        type="text"
                        bind:value={onboardProxyUsername}
                        class="w-full px-3 py-1.5 text-sm bg-white border border-slate-200 rounded-lg focus:ring-2 focus:ring-[#003ec7]"
                      />
                    </div>

                    <div class="flex flex-col gap-1.5">
                      <label for="onboard-proxy-pass" class="text-[10px] font-bold text-slate-500 uppercase">Proxy Password (Optional)</label>
                      <input
                        id="onboard-proxy-pass"
                        type="password"
                        bind:value={onboardProxyPassword}
                        class="w-full px-3 py-1.5 text-sm bg-white border border-slate-200 rounded-lg focus:ring-2 focus:ring-[#003ec7]"
                      />
                    </div>
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={onboardStatus === "loading"}
                  class="w-full py-3 bg-[#003ec7] hover:bg-blue-800 disabled:opacity-50 text-white font-bold rounded-xl shadow-md transition-all flex items-center justify-center gap-2"
                >
                  {#if onboardStatus === "loading"}
                    <span class="animate-spin h-5 w-5 border-2 border-white border-t-transparent rounded-full"></span>
                    Processing Authentication...
                  {:else}
                    <span class="material-symbols-outlined">verified_user</span>
                    Authenticate and Register Session
                  {/if}
                </button>
              </form>
            {:else}
              <!-- Session File Upload Mode -->
              <form on:submit|preventDefault={submitSessionOnboarding} class="p-6 space-y-6">
                {#if onboardStatus === "success"}
                  <div class="p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl text-sm flex gap-2 items-start" role="alert">
                    <span class="material-symbols-outlined text-emerald-600">check_circle</span>
                    <div>
                      <p class="font-bold">Successfully Registered!</p>
                      <p class="text-xs mt-0.5">{onboardMessage}</p>
                    </div>
                  </div>
                {:else if onboardStatus === "error"}
                  <div class="p-4 bg-red-50 border border-red-200 text-red-800 rounded-xl text-sm flex gap-2 items-start" role="alert">
                    <span class="material-symbols-outlined text-red-600">error</span>
                    <div>
                      <p class="font-bold">Onboarding Failed</p>
                      <p class="text-xs mt-0.5">{onboardMessage}</p>
                    </div>
                  </div>
                {/if}

                <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <!-- Phone -->
                  <div class="flex flex-col gap-1.5">
                    <label for="session-phone" class="text-xs font-bold text-slate-700 uppercase tracking-wider">Phone Number</label>
                    <input
                      id="session-phone"
                      type="text"
                      placeholder="+1234567890"
                      bind:value={onboardPhone}
                      class="w-full px-3 py-2 text-sm border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#003ec7]"
                      required
                    />
                  </div>

                  <!-- Session File Zone -->
                  <div class="flex flex-col gap-1.5">
                    <span class="text-xs font-bold text-slate-700 uppercase tracking-wider">Session File</span>
                    {#if !sessionFile}
                      <div
                        role="button"
                        tabindex="0"
                        aria-label="Upload session file drag and drop zone"
                        class="relative w-full py-6 flex flex-col items-center justify-center rounded-xl bg-slate-50 border-2 border-dashed transition-all duration-200 group cursor-pointer hover:bg-slate-100 {isDragOver ? 'border-[#003ec7] bg-blue-50/20' : 'border-slate-300'}"
                        on:dragover|preventDefault={() => isDragOver = true}
                        on:dragleave|preventDefault={() => isDragOver = false}
                        on:drop|preventDefault={handleFileDrop}
                        on:keydown={handleKeyPress}
                      >
                        <div class="flex flex-col items-center gap-1.5 text-center px-4">
                          <span class="material-symbols-outlined text-[#003ec7] text-[24px]">file_upload</span>
                          <p class="text-xs font-semibold text-slate-700">Drag or click to upload</p>
                          <p class="text-[10px] text-slate-400 font-mono">.session, .tdata</p>
                        </div>
                        <input
                          bind:this={fileInputEl}
                          type="file"
                          accept=".session,.tdata"
                          class="absolute inset-0 opacity-0 cursor-pointer"
                          on:change={handleFileSelect}
                        />
                      </div>
                    {:else}
                      <div class="p-3 bg-slate-100 rounded-xl flex items-center justify-between border border-slate-200">
                        <div class="flex items-center gap-2 min-w-0">
                          <span class="material-symbols-outlined text-slate-600 flex-shrink-0">description</span>
                          <div class="min-w-0">
                            <p class="text-xs font-semibold text-slate-800 truncate">{sessionFileName}</p>
                            <p class="text-[10px] text-slate-500">{sessionFileSize}</p>
                          </div>
                        </div>
                        <button
                          type="button"
                          class="text-red-600 hover:bg-red-50 p-1 rounded-full transition-colors flex items-center flex-shrink-0"
                          on:click={removeSessionFile}
                          aria-label="Remove selected file"
                        >
                          <span class="material-symbols-outlined text-[18px]">close</span>
                        </button>
                      </div>
                    {/if}

                    {#if sessionFileValidationError}
                      <p class="text-[11px] font-semibold text-red-600 flex items-center gap-1 mt-1">
                        <span class="material-symbols-outlined text-[12px]">error_outline</span>
                        {sessionFileValidationError}
                      </p>
                    {/if}
                  </div>
                </div>

                <!-- Proxy Settings Card -->
                <div class="border border-slate-100 bg-slate-50 p-4 rounded-xl space-y-4">
                  <h3 class="text-xs font-bold text-slate-700 uppercase tracking-wider flex items-center gap-1.5">
                    <span class="material-symbols-outlined text-[16px]">settings_ethernet</span>
                    Proxy Configuration
                  </h3>

                  <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <div class="flex flex-col gap-1.5">
                      <label for="session-proxy-ip" class="text-[10px] font-bold text-slate-500 uppercase">Proxy IP</label>
                      <input
                        id="session-proxy-ip"
                        type="text"
                        bind:value={onboardProxyIp}
                        class="w-full px-3 py-1.5 text-sm bg-white border border-slate-200 rounded-lg focus:ring-2 focus:ring-[#003ec7]"
                        required
                      />
                    </div>

                    <div class="flex flex-col gap-1.5">
                      <label for="session-proxy-port" class="text-[10px] font-bold text-slate-500 uppercase">Port</label>
                      <input
                        id="session-proxy-port"
                        type="number"
                        bind:value={onboardProxyPort}
                        class="w-full px-3 py-1.5 text-sm bg-white border border-slate-200 rounded-lg focus:ring-2 focus:ring-[#003ec7]"
                        required
                      />
                    </div>

                    <div class="flex flex-col gap-1.5">
                      <label for="session-proxy-protocol" class="text-[10px] font-bold text-slate-500 uppercase">Protocol</label>
                      <select
                        id="session-proxy-protocol"
                        bind:value={onboardProxyProtocol}
                        class="w-full px-3 py-1.5 text-sm bg-white border border-slate-200 rounded-lg focus:ring-2 focus:ring-[#003ec7]"
                      >
                        <option value="HTTP">HTTP</option>
                        <option value="SOCKS5">SOCKS5</option>
                      </select>
                    </div>
                  </div>

                  <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div class="flex flex-col gap-1.5">
                      <label for="session-proxy-user" class="text-[10px] font-bold text-slate-500 uppercase">Proxy Username (Optional)</label>
                      <input
                        id="session-proxy-user"
                        type="text"
                        bind:value={onboardProxyUsername}
                        class="w-full px-3 py-1.5 text-sm bg-white border border-slate-200 rounded-lg focus:ring-2 focus:ring-[#003ec7]"
                      />
                    </div>

                    <div class="flex flex-col gap-1.5">
                      <label for="session-proxy-pass" class="text-[10px] font-bold text-slate-500 uppercase">Proxy Password (Optional)</label>
                      <input
                        id="session-proxy-pass"
                        type="password"
                        bind:value={onboardProxyPassword}
                        class="w-full px-3 py-1.5 text-sm bg-white border border-slate-200 rounded-lg focus:ring-2 focus:ring-[#003ec7]"
                      />
                    </div>
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={onboardStatus === "loading"}
                  class="w-full py-3 bg-[#003ec7] hover:bg-blue-800 disabled:opacity-50 text-white font-bold rounded-xl shadow-md transition-all flex items-center justify-center gap-2"
                >
                  {#if onboardStatus === "loading"}
                    <span class="animate-spin h-5 w-5 border-2 border-white border-t-transparent rounded-full"></span>
                    Uploading Session file...
                  {:else}
                    <span class="material-symbols-outlined">cloud_upload</span>
                    Onboard Session File
                  {/if}
                </button>
              </form>
            {/if}
          </div>
        </main>
      {/if}

      <!-- VIEW 3: LEAD CSV INGESTION MODULE -->
      {#if currentTab === 'ingest'}
        <main class="flex-1 overflow-y-auto p-4 md:p-8 bg-slate-50">
          <div class="max-w-2xl mx-auto bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
            <header class="bg-[#003ec7] text-white p-6">
              <div class="flex items-center gap-3">
                <span class="material-symbols-outlined text-[28px]">upload_file</span>
                <h2 class="font-bold text-lg md:text-xl">Import Target Outreach Leads</h2>
              </div>
              <p class="text-xs text-blue-100 mt-1">Associate list targets directly with specific platform marketing campaigns for AI execution.</p>
            </header>

            <form on:submit|preventDefault={submitIngestLeads} class="p-6 space-y-6">
              {#if ingestStatus === "success"}
                <div class="p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl text-sm flex gap-2 items-start" role="alert">
                  <span class="material-symbols-outlined text-emerald-600">check_circle</span>
                  <div>
                    <p class="font-bold">Ingestion Successful!</p>
                    <p class="text-xs mt-0.5">{ingestMessage}</p>
                  </div>
                </div>
              {:else if ingestStatus === "error"}
                <div class="p-4 bg-red-50 border border-red-200 text-red-800 rounded-xl text-sm flex gap-2 items-start" role="alert">
                  <span class="material-symbols-outlined text-red-600">error</span>
                  <div>
                    <p class="font-bold">Ingestion Failed</p>
                    <p class="text-xs mt-0.5">{ingestMessage}</p>
                  </div>
                </div>
              {/if}

              <!-- Campaign Selector or Creator -->
              <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                <!-- Select existing -->
                <div class="flex flex-col gap-1.5">
                  <label for="ingest-campaign-select" class="text-xs font-bold text-slate-700 uppercase tracking-wider">Select Active Campaign</label>
                  <select
                    id="ingest-campaign-select"
                    bind:value={selectedCampaignId}
                    class="w-full px-3 py-2 text-sm border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#003ec7] bg-white"
                    disabled={!!newCampaignName.trim()}
                  >
                    {#if campaigns.length === 0}
                      <option value="">No campaigns found. Create one first!</option>
                    {:else}
                      {#each campaigns as camp}
                        <option value={camp.id}>{camp.name}</option>
                      {/each}
                    {/if}
                  </select>
                  <p class="text-[10px] text-slate-500">Only available if not typing a new campaign name below.</p>
                </div>

                <!-- Create new Campaign inline -->
                <div class="flex flex-col gap-1.5 border-l-0 md:border-l md:pl-6 border-slate-100">
                  <label for="ingest-campaign-new" class="text-xs font-bold text-slate-700 uppercase tracking-wider flex items-center gap-1">
                    <span class="material-symbols-outlined text-[15px] text-blue-600">add_circle</span>
                    Or Create New Campaign
                  </label>
                  <input
                    id="ingest-campaign-new"
                    type="text"
                    placeholder="E.g., APAC Winter Promo"
                    bind:value={newCampaignName}
                    class="w-full px-3 py-2 text-sm border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#003ec7]"
                  />
                </div>
              </div>

              <!-- Optional Spintax rules for new campaign -->
              {#if newCampaignName.trim()}
                <div class="flex flex-col gap-1.5 p-4 bg-blue-50/50 rounded-xl border border-blue-100">
                  <label for="ingest-spintax" class="text-xs font-bold text-slate-700 uppercase">Spintax template Rules</label>
                  <input
                    id="ingest-spintax"
                    type="text"
                    bind:value={newCampaignSpintax}
                    class="w-full px-3 py-1.5 text-sm border border-slate-200 bg-white rounded-lg focus:ring-2 focus:ring-[#003ec7]"
                  />
                  <p class="text-[10px] text-slate-500">Spintax templates are evaluated per lead (e.g. {`{Hi|Hey|Hello}`}).</p>
                </div>
              {/if}

              <!-- CSV / Target list text area -->
              <div class="flex flex-col gap-1.5">
                <label for="ingest-csv" class="text-xs font-bold text-slate-700 uppercase tracking-wider flex justify-between items-center">
                  <span>Leads Target List (CSV / Raw text)</span>
                  <span class="text-[10px] font-normal text-slate-500 italic">username, phoneNumber, metadata</span>
                </label>
                <textarea
                  id="ingest-csv"
                  rows="8"
                  placeholder={`@username_one,+1234567890,interested in core tiers\n@username_two,,needs direct assistance\n,+1987654321,cold outreach trial`}
                  bind:value={csvContent}
                  class="w-full p-4 bg-slate-50 font-mono text-xs border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-[#003ec7]"
                  required
                ></textarea>
              </div>

              <button
                type="submit"
                disabled={ingestStatus === "loading"}
                class="w-full py-3 bg-[#003ec7] hover:bg-blue-800 disabled:opacity-50 text-white font-bold rounded-xl shadow-md transition-all flex items-center justify-center gap-2"
              >
                {#if ingestStatus === "loading"}
                  <span class="animate-spin h-5 w-5 border-2 border-white border-t-transparent rounded-full"></span>
                  Ingesting Targets and Scheduling...
                {:else}
                  <span class="material-symbols-outlined">cloud_upload</span>
                  Upload and Ingest Leads
                {/if}
              </button>
            </form>
          </div>
        </main>
      {/if}
    </div>
  {/if}
</div>

<style>
  /* Add custom scrollbar and animation styling */
  textarea {
    resize: none;
  }
</style>
