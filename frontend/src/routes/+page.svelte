<script lang="ts">
    interface Account {
        id: number;
        phoneNumber: string;
        status: 'Active' | 'Temporary Spam-Block' | 'Permanent Ban' | 'Re-authorization Required';
    }

    let accounts: Account[] = $state([
        { id: 1, phoneNumber: '+1234567890', status: 'Active' },
        { id: 2, phoneNumber: '+1987654321', status: 'Temporary Spam-Block' },
        { id: 3, phoneNumber: '+1555555555', status: 'Permanent Ban' }
    ]);

    let uploadProgress = $state(0);
    let isUploading = $state(false);
    let fileInput: HTMLInputElement;

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
</script>

<main class="min-h-screen bg-gray-50 p-4 md:p-8 font-sans text-gray-900">
    <div class="max-w-6xl mx-auto space-y-8">

        <header>
            <h1 class="text-3xl font-bold text-gray-900 tracking-tight">Account Management Dashboard</h1>
            <p class="text-gray-500 mt-2">Manage your Telegram outreach accounts and monitor their health status.</p>
        </header>

        <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">

            <!-- Accounts List Section -->
            <section class="lg:col-span-2 space-y-4" aria-labelledby="accounts-heading">
                <h2 id="accounts-heading" class="text-xl font-semibold text-gray-800 border-b border-gray-200 pb-2">Connected Accounts</h2>

                <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {#each accounts as account (account.id)}
                        <article class="bg-white p-5 rounded-xl shadow-sm border border-gray-100 hover:shadow-md transition-shadow duration-200 flex flex-col justify-between h-full">
                            <div class="flex justify-between items-start mb-4">
                                <h3 class="font-medium text-gray-900 truncate pr-2" title={account.phoneNumber}>{account.phoneNumber}</h3>
                                <span class={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border ${getStatusColor(account.status)}`} aria-label={`Status: ${account.status}`}>
                                    {account.status}
                                </span>
                            </div>
                            <div class="flex items-center text-sm text-gray-500">
                                <svg class="w-4 h-4 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5.121 17.804A13.937 13.937 0 0112 16c2.5 0 4.847.655 6.879 1.804M15 10a3 3 0 11-6 0 3 3 0 016 0zm6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
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
                <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6 sticky top-8">
                    <h2 class="text-lg font-semibold text-gray-800 mb-4">Add New Account</h2>

                    <form class="space-y-5" onsubmit={(e) => e.preventDefault()}>
                        <div>
                            <label for="tdata-upload" class="block text-sm font-medium text-gray-700 mb-2">
                                Upload Session/tdata File
                            </label>
                            <div class="mt-1 flex justify-center px-6 pt-5 pb-6 border-2 border-gray-300 border-dashed rounded-lg hover:bg-gray-50 transition-colors">
                                <div class="space-y-1 text-center">
                                    <svg class="mx-auto h-12 w-12 text-gray-400" stroke="currentColor" fill="none" viewBox="0 0 48 48" aria-hidden="true">
                                        <path d="M28 8H12a4 4 0 00-4 4v20m32-12v8m0 0v8a4 4 0 01-4 4H12a4 4 0 01-4-4v-4m32-4l-3.172-3.172a4 4 0 00-5.656 0L28 28M8 32l9.172-9.172a4 4 0 015.656 0L28 28m0 0l4 4m4-24h8m-4-4v8m-12 4h.02" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                                    </svg>
                                    <div class="flex text-sm text-gray-600 justify-center">
                                        <label for="tdata-upload" class="relative cursor-pointer bg-white rounded-md font-medium text-blue-600 hover:text-blue-500 focus-within:outline-none focus-within:ring-2 focus-within:ring-offset-2 focus-within:ring-blue-500">
                                            <span>Select a file</span>
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
                                        </label>
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
                                <div class="flex justify-between text-sm text-gray-600 mb-1">
                                    <span>Uploading...</span>
                                    <span>{uploadProgress}%</span>
                                </div>
                                <progress
                                    class="w-full h-2 rounded-full overflow-hidden [&::-webkit-progress-bar]:bg-gray-100 [&::-webkit-progress-value]:bg-blue-500 [&::-moz-progress-bar]:bg-blue-500"
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
    </div>
</main>
