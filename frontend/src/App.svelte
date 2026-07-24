<script>
  import { tick, onMount } from 'svelte';

  // Core reactive data state
  let chats = [
    {
      id: "chat-1",
      title: "Urgent: Acme Corp Expansion",
      clientName: "Sarah Jenkins",
      avatar: "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&q=80&w=120",
      time: "2m ago",
      escalated: true,
      priority: "critical",
      unreadCount: 1,
      lastMessage: "The prospect is concerned about pricing tiers for the APAC region...",
      messages: [
        { id: "m1", sender: "client", text: "Hello! I received your automated offer about APAC licensing.", time: "10:00 AM" },
        { id: "m2", sender: "ai", text: "Hello Sarah! Thanks for reaching out. Yes, we offer tier-based volume discounts for APAC. What is your estimated seat size?", time: "10:01 AM" },
        { id: "m3", sender: "client", text: "We need 500+ licenses but your pricing page only lists up to 100. The prospect is concerned about pricing tiers for the APAC region. Can we get custom terms?", time: "10:02 AM" }
      ]
    },
    {
      id: "chat-2",
      title: "Global Logistics Deal",
      clientName: "Marcus Thorne",
      avatar: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&q=80&w=120",
      time: "14m ago",
      escalated: true,
      priority: "high",
      unreadCount: 0,
      lastMessage: "Contract legal review is pending signature. Need immediate sync.",
      messages: [
        { id: "m4", sender: "client", text: "Hello, we are review the contract now.", time: "9:30 AM" },
        { id: "m5", sender: "ai", text: "Great Marcus, let me know if you have any questions about the liability clauses.", time: "9:32 AM" },
        { id: "m6", sender: "client", text: "Contract legal review is pending signature. Need immediate sync. Our legal counsel has a concern.", time: "9:45 AM" }
      ]
    },
    {
      id: "chat-3",
      title: "TechStart SaaS Renewal",
      clientName: "Elena Rodriguez",
      avatar: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=120",
      time: "1h ago",
      escalated: false,
      priority: "normal",
      unreadCount: 0,
      lastMessage: "The renewal invoice has been sent. Let's touch base on Friday.",
      messages: [
        { id: "m7", sender: "operator", text: "Hi Elena, I've sent over the invoice for next year's renewal.", time: "Yesterday" },
        { id: "m8", sender: "client", text: "The renewal invoice has been sent. Let's touch base on Friday. Thanks!", time: "9:00 AM" }
      ]
    },
    {
      id: "chat-4",
      title: "Innovate Ltd",
      clientName: "David Chen",
      avatar: "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&q=80&w=120",
      time: "5h ago",
      escalated: false,
      priority: "closed",
      unreadCount: 0,
      lastMessage: "Deal finalized. Handing over to implementation team.",
      messages: [
        { id: "m9", sender: "operator", text: "Congrats on signing! Welcome aboard.", time: "Yesterday" },
        { id: "m10", sender: "client", text: "Thank you so much! Deal finalized. Handing over to implementation team.", time: "Yesterday" }
      ]
    }
  ];

  let selectedChatId = "chat-1"; // default active chat to ensure instant display on desktop
  let newMessageText = "";
  let filterType = "all"; // 'all' | 'escalated' | 'regular' | 'closed'
  let searchQuery = "";
  let messageContainer;

  // Reactivity
  $: activeChat = chats.find(c => c.id === selectedChatId);
  $: escalatedCount = chats.filter(c => c.escalated).length;
  $: activeDealsCount = chats.filter(c => c.priority !== 'closed').length;

  $: filteredChats = chats.filter(chat => {
    // Search filter
    const query = searchQuery.toLowerCase().trim();
    const matchesSearch = !query ||
      chat.title.toLowerCase().includes(query) ||
      chat.clientName.toLowerCase().includes(query) ||
      chat.lastMessage.toLowerCase().includes(query);

    if (!matchesSearch) return false;

    // Category filter
    if (filterType === "escalated") return chat.escalated;
    if (filterType === "regular") return !chat.escalated && chat.priority !== "closed";
    if (filterType === "closed") return chat.priority === "closed";
    return true;
  });

  async function selectChat(id) {
    selectedChatId = id;
    await tick();
    scrollChatToBottom();
  }

  function scrollChatToBottom() {
    if (messageContainer) {
      messageContainer.scrollTop = messageContainer.scrollHeight;
    }
  }

  async function handleSendMessage() {
    if (!newMessageText.trim()) return;

    const currentChat = chats.find(c => c.id === selectedChatId);
    if (currentChat) {
      const now = new Date();
      const timeStr = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

      currentChat.messages = [
        ...currentChat.messages,
        {
          id: `m-user-${Date.now()}`,
          sender: "operator",
          text: newMessageText.trim(),
          time: timeStr
        }
      ];
      currentChat.lastMessage = newMessageText.trim();
      currentChat.time = "Just now";

      // trigger Svelte reactivity
      chats = [...chats];
      newMessageText = "";

      await tick();
      scrollChatToBottom();
    }
  }

  function handleKeydown(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      handleSendMessage();
    }
  }

  // Lifecycle
  onMount(() => {
    scrollChatToBottom();
  });
</script>

<div class="min-h-screen bg-slate-50 text-slate-900 flex flex-col">
  <!-- Header Application Shell -->
  <header class="bg-[#003ec7] text-white h-16 px-4 md:px-6 flex items-center justify-between shadow-md z-10">
    <div class="flex items-center gap-3">
      <span class="material-symbols-outlined text-[28px]" aria-hidden="true">forum</span>
      <h1 class="font-bold text-lg md:text-xl tracking-wide">LeadGen Bot Unified Inbox</h1>
    </div>
    <div class="flex items-center gap-4">
      <!-- Top Stats widgets for visual clarity -->
      <div class="hidden md:flex items-center gap-4 text-xs font-semibold">
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
        <span class="text-xs text-blue-100 hidden sm:inline">Operator Panel</span>
        <div class="w-8 h-8 rounded-full bg-blue-800 border-2 border-blue-300 flex items-center justify-center font-bold text-sm text-white" aria-label="Operator profile avatar">
          OP
        </div>
      </div>
    </div>
  </header>

  <!-- Responsive Layout Container -->
  <div class="flex-1 flex overflow-hidden">
    <!-- Chat List Sidebar (Visible on desktop, and visible on mobile only if no chat is active) -->
    <main class="w-full md:w-[380px] lg:w-[420px] flex flex-col border-r border-slate-200 bg-white {selectedChatId && 'hidden md:flex'}">
      <!-- Mobile Stats widgets -->
      <div class="grid grid-cols-2 gap-2 p-3 bg-slate-50 md:hidden">
        <div class="bg-yellow-50 border border-yellow-300 p-2.5 rounded-xl flex flex-col">
          <span class="text-[10px] text-yellow-800 font-bold uppercase tracking-wider">Escalations</span>
          <span class="text-xl font-bold text-yellow-700">{escalatedCount} Active</span>
        </div>
        <div class="bg-blue-50 border border-blue-200 p-2.5 rounded-xl flex flex-col">
          <span class="text-[10px] text-blue-800 font-bold uppercase tracking-wider">Active Deals</span>
          <span class="text-xl font-bold text-blue-700">{activeDealsCount} Open</span>
        </div>
      </div>

      <!-- Search and Filter Bar -->
      <div class="p-3 border-b border-slate-200 space-y-2 bg-white">
        <!-- Search -->
        <div class="relative">
          <span class="material-symbols-outlined absolute left-3 top-2.5 text-slate-400 text-[20px]" aria-hidden="true">search</span>
          <input
            type="text"
            placeholder="Search leads or messages..."
            bind:value={searchQuery}
            class="w-full pl-9 pr-3 py-2 text-sm bg-slate-50 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#003ec7] focus:bg-white transition-all"
            aria-label="Search conversations"
          />
        </div>

        <!-- Semantic Segmented Filter Tabs -->
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
        {#if filteredChats.length === 0}
          <div class="p-8 text-center text-slate-400 text-sm">
            <span class="material-symbols-outlined text-[36px] mb-1" aria-hidden="true">chat_bubble_outline</span>
            <p>No conversations found</p>
          </div>
        {:else}
          {#each filteredChats as chat (chat.id)}
            <!-- Chat Item Card -->
            <!-- Accessible button with outline focus ring and semantically structured contents -->
            <button
              on:click={() => selectChat(chat.id)}
              class="w-full text-left p-4 flex gap-3 transition-all relative outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-[#003ec7] z-0
                {chat.id === selectedChatId ? 'bg-blue-50/50' : 'hover:bg-slate-50'}
                {chat.escalated ? 'bg-yellow-50/95 escalated-pulse border-l-4 border-yellow-500' : 'border-l-4 border-transparent'}"
              aria-label="Chat with {chat.clientName}, {chat.title}. {chat.escalated ? 'Escalated.' : ''}"
            >
              <!-- Left marker for Escalation background color coding visual accessibility -->
              {#if chat.escalated}
                <div class="absolute left-0 top-0 bottom-0 w-1 bg-yellow-500" aria-hidden="true"></div>
              {/if}

              <!-- Avatar -->
              <div class="relative flex-shrink-0">
                <img
                  src={chat.avatar}
                  alt={chat.clientName}
                  class="w-12 h-12 rounded-full object-cover border border-slate-200"
                />
                {#if chat.escalated}
                  <div class="absolute -bottom-1 -right-1 bg-yellow-500 text-slate-950 rounded-full p-0.5 border-2 border-white flex items-center justify-center" aria-hidden="true">
                    <span class="material-symbols-outlined text-[13px] font-bold">priority_high</span>
                  </div>
                {:else if chat.unreadCount > 0}
                  <div class="absolute -bottom-1 -right-1 bg-blue-600 text-white rounded-full w-4.5 h-4.5 text-[10px] font-bold border-2 border-white flex items-center justify-center">
                    {chat.unreadCount}
                  </div>
                {/if}
              </div>

              <!-- Message Details -->
              <div class="flex-1 min-w-0">
                <div class="flex justify-between items-baseline mb-1">
                  <h2 class="font-semibold text-sm text-slate-900 truncate">{chat.title}</h2>
                  <span class="text-xs text-slate-500 flex-shrink-0">{chat.time}</span>
                </div>

                <div class="flex items-center gap-2 mb-1.5">
                  <span class="text-xs font-medium text-slate-600">{chat.clientName}</span>
                  {#if chat.escalated}
                    <span class="bg-yellow-200 text-yellow-900 border border-yellow-400 font-bold px-1.5 py-0.5 rounded text-[9px] uppercase tracking-wider flex items-center gap-0.5">
                      <span class="material-symbols-outlined text-[10px]">auto_awesome</span>
                      AI Flagged
                    </span>
                  {:else if chat.priority === 'high'}
                    <span class="bg-red-50 text-red-600 border border-red-200 font-bold px-1.5 py-0.5 rounded text-[9px] uppercase tracking-wider">
                      High Priority
                    </span>
                  {:else if chat.priority === 'closed'}
                    <span class="bg-slate-100 text-slate-600 border border-slate-200 font-semibold px-1.5 py-0.5 rounded text-[9px] uppercase tracking-wider">
                      Closed
                    </span>
                  {/if}
                </div>

                <p class="text-xs text-slate-500 truncate {chat.escalated ? 'text-slate-800 font-medium italic' : ''}">
                  {chat.lastMessage}
                </p>
              </div>
            </button>
          {/each}
        {/if}
      </section>
    </main>

    <!-- Chat Window Panel (Visible on desktop, and visible on mobile only if a chat is active) -->
    <section class="flex-1 flex flex-col bg-slate-100 {!selectedChatId && 'hidden md:flex'} {selectedChatId ? 'flex' : 'hidden'}">
      {#if activeChat}
        <!-- Active Chat Header -->
        <header class="bg-white border-b border-slate-200 h-16 px-4 flex items-center justify-between shadow-sm flex-shrink-0">
          <div class="flex items-center gap-3">
            <!-- Mobile Back Button -->
            <button
              on:click={() => selectedChatId = null}
              class="md:hidden p-2 -ml-2 rounded-full hover:bg-slate-100 text-slate-600 focus-visible:ring-2 focus-visible:ring-[#003ec7] outline-none"
              aria-label="Back to chat list"
            >
              <span class="material-symbols-outlined text-[24px]">arrow_back</span>
            </button>

            <img
              src={activeChat.avatar}
              alt={activeChat.clientName}
              class="w-10 h-10 rounded-full object-cover border border-slate-100"
            />
            <div>
              <div class="flex items-center gap-2">
                <h2 class="font-bold text-sm text-slate-900">{activeChat.clientName}</h2>
                {#if activeChat.escalated}
                  <span class="bg-yellow-100 text-yellow-800 border border-yellow-300 px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider flex items-center gap-0.5">
                    <span class="material-symbols-outlined text-[11px]" style="font-variation-settings: 'FILL' 1;">warning</span>
                    AI Escalated
                  </span>
                {/if}
              </div>
              <p class="text-xs text-slate-500 truncate max-w-[200px] sm:max-w-md">{activeChat.title}</p>
            </div>
          </div>

          <div class="flex items-center gap-2">
            <!-- Action to mark deal closed / solve escalation -->
            {#if activeChat.priority !== 'closed'}
              <button
                on:click={() => {
                  activeChat.priority = 'closed';
                  activeChat.escalated = false;
                  chats = [...chats];
                }}
                class="bg-emerald-600 hover:bg-emerald-700 text-white font-semibold text-xs px-3 py-1.5 rounded-lg flex items-center gap-1 shadow-sm transition-all focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-emerald-600 outline-none active:scale-95"
              >
                <span class="material-symbols-outlined text-[16px]">check_circle</span>
                Close Deal
              </button>
            {:else}
              <span class="text-emerald-600 font-bold text-xs flex items-center gap-1 px-3 py-1.5 bg-emerald-50 rounded-lg border border-emerald-200">
                <span class="material-symbols-outlined text-[16px]">check</span>
                Deal Finalized
              </span>
            {/if}
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
          <!-- Announcement banner for escalated status -->
          {#if activeChat.escalated}
            <div class="bg-yellow-50 border border-yellow-200 rounded-xl p-3 text-center max-w-lg mx-auto shadow-sm">
              <div class="flex items-center justify-center gap-1.5 text-yellow-800 font-bold text-xs uppercase tracking-wider mb-1">
                <span class="material-symbols-outlined text-[16px]">auto_awesome</span>
                AI Agent Handoff Required
              </div>
              <p class="text-xs text-yellow-900 leading-relaxed">
                The lead requested custom pricing/APAC region parameters. The automated dialog has been paused. Please respond manually in the composer below.
              </p>
            </div>
          {/if}

          <!-- Message bubbles -->
          {#each activeChat.messages as message (message.id)}
            <div class="flex {message.sender === 'operator' ? 'justify-end' : 'justify-start'}">
              <div class="max-w-[75%] sm:max-w-[65%] rounded-2xl px-4 py-2.5 shadow-sm text-sm
                {message.sender === 'operator'
                  ? 'bg-[#003ec7] text-white rounded-tr-none'
                  : message.sender === 'ai'
                    ? 'bg-amber-50 border border-amber-200 text-amber-950 rounded-tl-none relative before:content-[\'🤖_AI\'] before:block before:text-[9px] before:font-bold before:text-amber-700 before:mb-1'
                    : 'bg-white text-slate-800 border border-slate-200 rounded-tl-none'}"
              >
                <p class="leading-relaxed break-words">{message.text}</p>
                <span class="block text-[10px] mt-1 text-right {message.sender === 'operator' ? 'text-blue-200' : 'text-slate-400'}">
                  {message.time}
                </span>
              </div>
            </div>
          {/each}
        </div>

        <!-- Composer Area -->
        <footer class="bg-white border-t border-slate-200 p-3 md:p-4 flex-shrink-0">
          <div class="flex items-end gap-2 max-w-4xl mx-auto">
            <div class="flex-1 relative">
              <textarea
                rows="1"
                placeholder="Type your message to {activeChat.clientName}..."
                bind:value={newMessageText}
                on:keydown={handleKeydown}
                class="w-full bg-slate-50 border border-slate-200 focus:border-[#003ec7] focus:ring-2 focus:ring-[#003ec7] focus:bg-white rounded-xl py-2.5 pl-4 pr-10 text-sm resize-none outline-none transition-all max-h-32"
                aria-label="Type message"
              ></textarea>
              <button
                class="absolute right-3 bottom-2.5 text-slate-400 hover:text-[#003ec7] flex items-center justify-center p-1 rounded-full transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-[#003ec7]"
                aria-label="Add attachment"
              >
                <span class="material-symbols-outlined text-[20px]">attach_file</span>
              </button>
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
          <h2 class="font-bold text-slate-600 text-lg mb-1">Unified Inbox Ready</h2>
          <p class="text-sm max-w-sm">Select an escalated conversation from the sidebar list to start qualification and deal closing.</p>
        </div>
      {/if}
    </section>
  </div>
</div>
