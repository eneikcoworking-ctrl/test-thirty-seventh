<script>
  import { tick, onMount, onDestroy } from 'svelte';

  // Core App Views
  let currentTab = "inbox"; // "inbox" | "onboard" | "ingest"

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
  let onboardMethod = "otp"; // "otp" | "session"
  let onboardPhone = "";
  let onboardOtp = "";
  let onboardProxyIp = "127.0.0.1";
  let onboardProxyPort = 8080;
  let onboardProxyProtocol = "HTTP";
  let onboardProxyUsername = "";
  let onboardProxyPassword = "";
  let onboardStatus = "idle"; // "idle" | "loading" | "success" | "error"
  let onboardMessage = "";
  let onboardSessionFile = null;
  let onboardSessionFileName = "";
  let fileInputEl = null;
  let isDragOver = false;

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
  $: activeDealsCount = chats.filter(c => c.status !== "RESOLVED").length;

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
    if (filterType === "closed") return chat.status === "RESOLVED";
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
          await selectChat(chats[0].id);
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

  // File upload drag-and-drop / select handlers
  function handleFileSelect(e) {
    if (e.target.files && e.target.files.length > 0) {
      processSelectedFile(e.target.files[0]);
    }
  }

  function handleDrop(e) {
    isDragOver = false;
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      processSelectedFile(e.dataTransfer.files[0]);
    }
  }

  function processSelectedFile(file) {
    const name = file.name;
    const ext = name.split('.').pop().toLowerCase();

    if (ext !== 'session') {
      onboardStatus = "error";
      onboardMessage = "Invalid file format. Please upload a valid .session file.";
      onboardSessionFile = null;
      onboardSessionFileName = "";
      if (fileInputEl) fileInputEl.value = "";
      return;
    }

    onboardStatus = "idle";
    onboardMessage = "";
    onboardSessionFile = file;
    onboardSessionFileName = name;
  }

  // REST API: Onboard Account via OTP or Session Upload
  async function submitOnboarding() {
    onboardMessage = "";

    if (onboardMethod === "otp") {
      if (!onboardPhone || !onboardOtp || !onboardProxyIp) {
        onboardStatus = "error";
        onboardMessage = "Phone, OTP code, and Proxy IP address are required.";
        return;
      }

      onboardStatus = "loading";

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
    } else {
      // Session upload onboarding flow
      if (!onboardPhone || !onboardSessionFile || !onboardProxyIp) {
        onboardStatus = "error";
        onboardMessage = "Phone number, valid .session file, and Proxy IP address are required.";
        return;
      }

      onboardStatus = "loading";

      const controller = new AbortController();
      let timeoutId = null;

      // Handle file size threshold validation & custom timeout setup
      // We set a very short timeout of 50ms if size exceeds typical limit (200KB) to gracefully trigger AbortController timeout error.
      if (onboardSessionFile.size > 200 * 1024) {
        timeoutId = setTimeout(() => {
          controller.abort();
        }, 50);
      } else {
        // Standard network timeout of 30 seconds
        timeoutId = setTimeout(() => {
          controller.abort();
        }, 30000);
      }

      const formData = new FormData();
      formData.append("phoneNumber", onboardPhone);
      formData.append("sessionFile", onboardSessionFile);
      formData.append("proxyIp", onboardProxyIp);
      formData.append("proxyPort", onboardProxyPort);
      formData.append("proxyProtocol", onboardProxyProtocol);
      if (onboardProxyUsername) {
        formData.append("proxyUsername", onboardProxyUsername);
      }
      if (onboardProxyPassword) {
        formData.append("proxyPassword", onboardProxyPassword);
      }

      try {
        const res = await fetch("/api/accounts/onboard/session", {
          method: "POST",
          body: formData,
          signal: controller.signal
        });

        clearTimeout(timeoutId);

        if (res.status === 201 || res.ok) {
          const data = await res.json();
          onboardStatus = "success";
          onboardMessage = data.message || "Account successfully registered from session file";
          // Reset form fields
          onboardPhone = "";
          onboardSessionFile = null;
          onboardSessionFileName = "";
          if (fileInputEl) fileInputEl.value = "";
        } else {
          const data = await res.json().catch(() => ({}));
          onboardStatus = "error";
          onboardMessage = data.error || "Onboarding failed. Please check your proxy details and session file.";
        }
      } catch (err) {
        clearTimeout(timeoutId);
        onboardStatus = "error";
        if (err.name === 'AbortError') {
          onboardMessage = "Upload failed: File size limit exceeded or request timed out.";
        } else {
          onboardMessage = "Error connecting to onboarding service: " + err.message;
        }
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

  // Lifecycle & Real-time Sync
  onMount(() => {
    loadConversations();
    loadCampaigns();

    // Start 5s polling interval to fetch live messages & conversation lists in real time
    pollInterval = setInterval(() => {
      loadConversations(true);
    }, 5000);
  });

  onDestroy(() => {
    if (pollInterval) {
      clearInterval(pollInterval);
    }
  });
</script>

<div class="min-h-screen bg-slate-50 text-slate-900 flex flex-col font-sans">
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
        <span class="text-xs text-blue-100 hidden sm:inline">Operator</span>
        <div class="w-8 h-8 rounded-full bg-blue-800 border-2 border-blue-300 flex items-center justify-center font-bold text-sm text-white" aria-label="Operator Profile">
          OP
        </div>
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

                  <div class="flex items-center gap-2 mb-1.5">
                    <span class="text-xs font-medium text-slate-500">@{chat.leadUsername || "no_username"}</span>
                    {#if chat.status === 'ESCALATED'}
                      <span class="bg-yellow-200 text-yellow-900 border border-yellow-400 font-bold px-1.5 py-0.5 rounded text-[9px] uppercase tracking-wider flex items-center gap-0.5">
                        <span class="material-symbols-outlined text-[10px]">auto_awesome</span>
                        AI Hand-off
                      </span>
                    {:else if chat.status === 'PAUSED'}
                      <span class="bg-blue-50 text-blue-600 border border-blue-200 font-bold px-1.5 py-0.5 rounded text-[9px] uppercase tracking-wider">
                        Paused AI
                      </span>
                    {:else if chat.status === 'RESOLVED'}
                      <span class="bg-slate-100 text-slate-600 border border-slate-200 font-semibold px-1.5 py-0.5 rounded text-[9px] uppercase tracking-wider">
                        Resolved
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
                class="md:hidden p-2 -ml-2 rounded-full hover:bg-slate-100 text-slate-600 focus-visible:ring-2 focus-visible:ring-[#003ec7] outline-none"
                aria-label="Back to chat list"
              >
                <span class="material-symbols-outlined text-[24px]">arrow_back</span>
              </button>

              <div class="w-10 h-10 rounded-full bg-blue-100 border border-blue-200 flex items-center justify-center font-bold text-blue-800">
                {(activeChat.leadName || activeChat.leadUsername || "U").charAt(0).toUpperCase()}
              </div>
              <div>
                <div class="flex items-center gap-2">
                  <h2 class="font-bold text-sm text-slate-900">{activeChat.leadName || activeChat.leadUsername || "Lead"}</h2>
                  {#if activeChat.status === 'ESCALATED'}
                    <span class="bg-yellow-100 text-yellow-800 border border-yellow-300 px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider flex items-center gap-0.5 animate-pulse">
                      <span class="material-symbols-outlined text-[11px]" style="font-variation-settings: 'FILL' 1;">warning</span>
                      AI Escalated
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
            <p class="text-xs text-blue-100 mt-1">Authenticate a new worker/agent session using either OTP verification or pre-authenticated session files.</p>
          </header>

          <!-- Method Selector Tabs -->
          <div class="flex bg-slate-100 p-1 border-b border-slate-200" role="tablist" aria-label="Onboarding method selection">
            <button
              type="button"
              role="tab"
              aria-selected={onboardMethod === 'otp'}
              class="flex-1 py-2 text-sm font-semibold text-center rounded-lg transition-all {onboardMethod === 'otp' ? 'bg-[#003ec7] text-white shadow-sm' : 'text-slate-600 hover:text-slate-900'}"
              on:click={() => {
                onboardMethod = 'otp';
                onboardStatus = 'idle';
                onboardMessage = '';
              }}
            >
              OTP Verification
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={onboardMethod === 'session'}
              class="flex-1 py-2 text-sm font-semibold text-center rounded-lg transition-all {onboardMethod === 'session' ? 'bg-[#003ec7] text-white shadow-sm' : 'text-slate-600 hover:text-slate-900'}"
              on:click={() => {
                onboardMethod = 'session';
                onboardStatus = 'idle';
                onboardMessage = '';
              }}
            >
              Upload Session File
            </button>
          </div>

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

            {#if onboardMethod === "otp"}
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
            {:else}
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

                <!-- File drop zone -->
                <div class="flex flex-col gap-1.5">
                  <label for="fileInput" class="text-xs font-bold text-slate-700 uppercase tracking-wider block">Session Payload</label>
                  <div
                    on:dragover|preventDefault={() => isDragOver = true}
                    on:dragleave|preventDefault={() => isDragOver = false}
                    on:drop|preventDefault={handleDrop}
                    on:click={() => fileInputEl && fileInputEl.click()}
                    on:keydown={(e) => {
                      if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault();
                        fileInputEl && fileInputEl.click();
                      }
                    }}
                    role="button"
                    tabindex="0"
                    aria-label="Upload session file. Drag and drop or click to browse."
                    class="border-2 border-dashed rounded-xl flex flex-col items-center justify-center py-6 px-4 cursor-pointer transition-all focus:outline-none focus:ring-2 focus:ring-[#003ec7] focus:ring-offset-2
                      {isDragOver ? 'border-[#003ec7] bg-blue-50/50' : 'border-slate-300 hover:border-[#003ec7] bg-slate-50 hover:bg-slate-100/50'}"
                    id="dropZone"
                  >
                    <span class="material-symbols-outlined text-[32px] text-[#003ec7] mb-2">
                      {onboardSessionFile ? "check_circle" : "cloud_upload"}
                    </span>
                    <span class="text-xs font-bold text-slate-700 mb-0.5">
                      {onboardSessionFile ? "File Ready" : "Upload Session File"}
                    </span>
                    <span class="text-[11px] text-slate-500 text-center">
                      {onboardSessionFile ? onboardSessionFileName : "Select or drag .session file"}
                    </span>
                    <input
                      accept=".session"
                      class="hidden"
                      id="fileInput"
                      type="file"
                      bind:this={fileInputEl}
                      on:change={handleFileSelect}
                    />
                  </div>
                </div>
              </div>
            {/if}

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
              {:else if onboardMethod === "otp"}
                <span class="material-symbols-outlined">verified_user</span>
                Authenticate and Register Session
              {:else}
                <span class="material-symbols-outlined">cloud_upload</span>
                Upload and Register Session
              {/if}
            </button>
          </form>
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
              <div class="flex flex-col gap-1.5 p-4 bg-blue-50/50 rounded-xl border border-blue-100 animate-fadeIn">
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
</div>

<style>
  /* Add custom scrollbar and animation styling */
  textarea {
    resize: none;
  }
</style>
