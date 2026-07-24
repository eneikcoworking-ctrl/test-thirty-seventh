<script lang="ts">
    interface Account {
        id: number;
        phoneNumber: string;
        status: 'Active' | 'Temporary Spam-Block' | 'Permanent Ban' | 'Re-authorization Required';
    }

    let accounts = $state<Account[]>([
        { id: 1, phoneNumber: '+1234567890', status: 'Active' },
        { id: 2, phoneNumber: '+1987654321', status: 'Temporary Spam-Block' },
        { id: 3, phoneNumber: '+1555555555', status: 'Permanent Ban' }
    ]);

    let uploadProgress = $state(0);
    let isUploading = $state(false);
    let fileInput: HTMLInputElement;
    let isMobileMenuOpen = $state(false);

    function handleFileUpload(event: Event) {
        const input = event.target as HTMLInputElement;
        if (input.files && input.files.length > 0) {
            isUploading = true;
            uploadProgress = 0;

            // Simulate upload progress
            const interval = setInterval(() => {
                uploadProgress += 10;
                if (uploadProgress >= 100) {
                    clearInterval(interval);
                    isUploading = false;
                    uploadProgress = 0;
                    if (fileInput) fileInput.value = ''; // Reset input

                    // Simulate adding a new account
                    accounts = [...accounts, {
                        id: Math.floor(Math.random() * 10000),
                        phoneNumber: '+10000' + Math.floor(Math.random() * 10000),
                        status: 'Active'
                    }];
                }
            }, 200);
        }
    }

    function getStatusColor(status: Account['status']): string {
        switch (status) {
            case 'Active': return 'bg-green-100 text-green-800 border-green-200';
            case 'Temporary Spam-Block': return 'bg-yellow-100 text-yellow-800 border-yellow-200';
            case 'Permanent Ban': return 'bg-red-100 text-red-800 border-red-200';
            case 'Re-authorization Required': return 'bg-orange-100 text-orange-800 border-orange-200';
            default: return 'bg-gray-100 text-gray-800 border-gray-200';
        }
    }

    function handleKeyDown(event: KeyboardEvent) {
        if (event.key === 'Escape') {
            isMobileMenuOpen = false;
        }
    }
</script>

<div class="flex flex-col md:flex-row min-h-screen bg-gray-50 text-gray-900 font-sans">

    <!-- Top Bar for Mobile -->
    <header class="md:hidden flex items-center justify-between bg-white border-b border-gray-200 px-4 h-16 sticky top-0 z-30" aria-label="Mobile Navigation Bar">
        <div class="flex items-center gap-2">
            <span class="material-symbols-outlined text-indigo-600 font-bold">campaign</span>
            <span class="font-bold text-lg text-gray-800">LeadGen Bot</span>
        </div>
        <button
            type="button"
            class="p-2 rounded hover:bg-gray-100 text-gray-600 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            onclick={() => isMobileMenuOpen = !isMobileMenuOpen}
            aria-label="Toggle navigation menu"
            aria-expanded={isMobileMenuOpen}
        >
            <span class="material-symbols-outlined">{isMobileMenuOpen ? 'close' : 'menu'}</span>
        </button>
    </header>

    <!-- Navigation Drawer / Sidebar (Desktop) -->
    <aside class="hidden md:flex flex-col w-64 bg-white border-r border-gray-200 py-6 px-4 shrink-0" aria-label="Sidebar navigation">
        <div class="flex items-center gap-3 px-2 mb-8">
            <div class="w-10 h-10 bg-indigo-600 rounded-xl flex items-center justify-center text-white shadow-md shadow-indigo-200">
                <span class="material-symbols-outlined">campaign</span>
            </div>
            <div>
                <h1 class="font-bold text-base text-gray-900">LeadGen Bot</h1>
                <p class="text-xs text-gray-500">Enterprise Manager</p>
            </div>
        </div>

        <nav class="flex flex-col gap-1 flex-1">
            <a class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium bg-indigo-50 text-indigo-700 transition-colors" href="/" aria-label="Navigate to Account Dashboard" aria-current="page">
                <span class="material-symbols-outlined">dashboard</span>
                Dashboard
            </a>
            <a class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium text-gray-600 hover:bg-gray-50 hover:text-gray-900 transition-colors" href="/campaigns" aria-label="Navigate to Campaigns Manager">
                <span class="material-symbols-outlined">campaign</span>
                Campaigns
            </a>
        </nav>

        <div class="pt-4 border-t border-gray-200">
            <p class="text-[10px] text-gray-400 uppercase tracking-widest font-bold mb-1">Version</p>
            <p class="text-xs text-gray-600">v2.4.0</p>
        </div>
    </aside>

    <!-- Mobile Drawer -->
    {#if isMobileMenuOpen}
        <!-- svelte-ignore a11y_no_static_element_interactions -->
        <div
            class="md:hidden fixed inset-0 z-40 bg-gray-900/50 backdrop-blur-xs transition-opacity"
            onclick={() => isMobileMenuOpen = false}
            onkeydown={handleKeyDown}
        ></div>
        <aside class="md:hidden fixed top-16 bottom-0 left-0 w-64 bg-white border-r border-gray-200 z-50 flex flex-col p-4 shadow-xl" aria-label="Mobile Drawer navigation">
            <nav class="flex flex-col gap-1 flex-1">
                <a class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium bg-indigo-50 text-indigo-700 transition-colors" href="/" onclick={() => isMobileMenuOpen = false}>
                    <span class="material-symbols-outlined text-gray-400">dashboard</span>
                    Dashboard
                </a>
                <a class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium text-gray-600 hover:bg-gray-50 hover:text-gray-900 transition-colors" href="/campaigns" onclick={() => isMobileMenuOpen = false}>
                    <span class="material-symbols-outlined">campaign</span>
                    Campaigns
                </a>
            </nav>
            <div class="pt-4 border-t border-gray-100">
                <p class="text-[10px] text-gray-400 uppercase tracking-widest font-bold mb-1">Version</p>
                <p class="text-xs text-gray-600">v2.4.0</p>
            </div>
        </aside>
    {/if}

    <!-- Main Content Canvas -->
    <main class="flex-grow p-4 md:p-8 max-w-5xl mx-auto w-full pb-24">

        <header class="mb-8">
            <h1 class="text-2xl md:text-3xl font-bold text-gray-900 tracking-tight">Account Management Dashboard</h1>
            <p class="text-gray-500 mt-2 text-sm md:text-base">Manage your Telegram outreach accounts and monitor their health status.</p>
        </header>

        <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">

            <!-- Accounts List Section -->
            <section class="lg:col-span-2 space-y-4" aria-labelledby="accounts-heading">
                <h2 id="accounts-heading" class="text-lg font-bold text-gray-800 border-b border-gray-200 pb-2">Connected Accounts</h2>

                <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {#each accounts as account (account.id)}
                        <article class="bg-white p-5 rounded-xl shadow-xs border border-gray-200 hover:shadow-md transition-shadow duration-200 flex flex-col justify-between h-36">
                            <div class="flex justify-between items-start">
                                <h3 class="font-semibold text-gray-900 truncate pr-2 text-base" title={account.phoneNumber}>{account.phoneNumber}</h3>
                                <span class={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold border ${getStatusColor(account.status)}`} aria-label={`Status: ${account.status}`}>
                                    {account.status}
                                </span>
                            </div>
                            <div class="flex items-center text-xs text-gray-500 font-mono">
                                <span class="material-symbols-outlined text-[16px] mr-1.5 text-gray-400">fingerprint</span>
                                ID: {account.id}
                            </div>
                        </article>
                    {/each}

                    {#if accounts.length === 0}
                        <div class="col-span-1 md:col-span-2 bg-gray-50 border-2 border-dashed border-gray-200 rounded-xl p-8 text-center text-gray-500">
                            No accounts connected yet.
                        </div>
                    {/if}
                </div>
            </section>

            <!-- Add Account Section -->
            <aside class="lg:col-span-1">
                <div class="bg-white rounded-xl shadow-xs border border-gray-200 p-6 sticky top-8">
                    <h2 class="text-lg font-bold text-gray-800 mb-4">Add New Account</h2>

                    <form class="space-y-5" onsubmit={(e) => e.preventDefault()}>
                        <div>
                            <label for="tdata-upload" class="block text-sm font-semibold text-gray-700 mb-2">
                                Upload Session/tdata File
                            </label>
                            <div class="mt-1 flex justify-center px-6 pt-5 pb-6 border-2 border-gray-300 border-dashed rounded-xl hover:bg-gray-50 transition-colors relative">
                                <label for="tdata-upload" class="absolute inset-0 cursor-pointer w-full h-full opacity-0 z-10" aria-label="Upload session file"></label>
                                <div class="space-y-1 text-center">
                                    <span class="material-symbols-outlined text-gray-400 text-4xl">upload_file</span>
                                    <div class="flex text-sm text-gray-600 justify-center">
                                        <span class="relative font-semibold text-indigo-600 hover:text-indigo-500">
                                            Select a file
                                            <input
                                                id="tdata-upload"
                                                name="tdata-upload"
                                                type="file"
                                                class="sr-only"
                                                accept=".session,.tdata"
                                                bind:this={fileInput}
                                                onchange={handleFileUpload}
                                                disabled={isUploading}
                                                aria-describedby="file-upload-help"
                                            >
                                        </span>
                                        <p class="pl-1">or drag and drop</p>
                                    </div>
                                    <p id="file-upload-help" class="text-xs text-gray-500">
                                        .session or JSON up to 10MB
                                    </p>
                                </div>
                            </div>
                        </div>

                        {#if isUploading}
                            <div class="mt-4" aria-live="polite">
                                <div class="flex justify-between text-xs font-semibold text-gray-600 mb-1">
                                    <span>Uploading...</span>
                                    <span>{uploadProgress}%</span>
                                </div>
                                <progress
                                    class="w-full h-2 rounded-full overflow-hidden [&::-webkit-progress-bar]:bg-gray-100 [&::-webkit-progress-value]:bg-indigo-600 [&::-moz-progress-bar]:bg-indigo-600"
                                    value={uploadProgress}
                                    max="100"
                                    aria-label="Upload progress"
                                >
                                    {uploadProgress}%
                                </progress>
                            </div>
                        {/if}
                    </form>
                </div>
            </aside>

        </div>
    </main>
</div>
