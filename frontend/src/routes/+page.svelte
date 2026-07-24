<script lang="ts">
    // State
    let agentName = $state('');
    let personaDescription = $state('');
    let voiceSpectrum = $state<'formal' | 'casual'>('formal');
    let traits = $state({
        enthusiastic: true,
        concise: false,
        empathetic: true,
        technical: false
    });

    type StopRule = {
        id: number;
        name: string;
        description: string;
        active: boolean;
    };

    let stopRules = $state<StopRule[]>([
        { id: 1, name: 'Complex Inquiry detected', description: 'Escalate when logic exceeds N-steps', active: true },
        { id: 2, name: 'Negative Sentiment detected', description: 'Triggered by persistent frustration', active: true },
        { id: 3, name: 'Request for human agent', description: 'Mandatory intervention trigger', active: true }
    ]);

    function addCustomRule() {
        stopRules = [...stopRules, {
            id: Date.now(),
            name: 'New Custom Rule',
            description: 'Custom trigger conditions',
            active: true
        }];
    }

    function handleSave() {
        console.log('Saved configuration', {
            agentName,
            personaDescription
        });
    }
</script>

<svelte:head>
    <title>AI System Prompt Configurator</title>
</svelte:head>

<main class="min-h-screen bg-background text-on-background font-sans pb-24">
    <!-- Header -->
    <header class="sticky top-0 z-40 bg-surface/80 backdrop-blur-md border-b border-outline-variant px-container-padding py-md flex items-center justify-between">
        <div class="flex items-center gap-2">
            <span class="material-symbols-outlined text-primary text-[28px]">robot_2</span>
            <h1 class="text-headline-md font-headline-md text-on-surface">AI Behavior</h1>
        </div>
        <button onclick={handleSave} class="bg-primary text-on-primary px-4 py-2 rounded-full font-label-md text-label-md hover:bg-primary/90 transition-colors active:scale-95 shadow-sm">
            Save
        </button>
    </header>

    <!-- Main Content Container -->
    <div class="px-container-padding py-lg space-y-xl max-w-3xl mx-auto">

        <!-- Agent Persona Section -->
        <section class="space-y-sm">
            <h2 class="text-label-md font-label-md text-secondary uppercase tracking-wider px-1">Agent Persona</h2>

            <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-md space-y-md">
                <!-- Agent Name -->
                <div class="space-y-xs flex flex-col">
                    <label for="agentName" class="text-label-sm font-label-sm text-secondary px-1">Agent Name</label>
                    <input
                        id="agentName"
                        type="text"
                        bind:value={agentName}
                        placeholder="e.g. Travel Concierge"
                        class="w-full bg-surface border border-outline focus:border-primary focus:ring-1 focus:ring-primary rounded-lg px-md py-3 text-body-md text-on-surface transition-all placeholder:text-outline"
                    />
                </div>

                <!-- Persona Description -->
                <div class="space-y-xs flex flex-col">
                    <label for="personaDescription" class="text-label-sm font-label-sm text-secondary px-1">Persona Description</label>
                    <textarea
                        id="personaDescription"
                        bind:value={personaDescription}
                        placeholder="Describe the agent's background, purpose, and key behaviors..."
                        rows="4"
                        class="w-full bg-surface border border-outline focus:border-primary focus:ring-1 focus:ring-primary rounded-lg px-md py-3 text-body-md text-on-surface transition-all placeholder:text-outline resize-none"
                    ></textarea>
                    <p class="text-label-sm font-label-sm text-on-surface-variant italic opacity-70 px-1">Tip: Define specific knowledge boundaries here.</p>
                </div>
            </div>
        </section>

        <!-- Tone of Voice Section -->
        <section class="space-y-sm">
            <h2 class="text-label-md font-label-md text-secondary uppercase tracking-wider px-1">Tone of Voice</h2>

            <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-md space-y-lg">

                <!-- Segmented Slider -->
                <fieldset class="space-y-xs border-0 p-0 m-0">
                    <legend class="text-label-sm font-label-sm text-secondary px-1 mb-2 block w-full">Voice Spectrum</legend>
                    <div class="relative flex bg-surface-container border border-outline-variant rounded-full p-1 h-12">
                        <div
                            class="segmented-indicator absolute top-1 bottom-1 w-[calc(50%-4px)] bg-primary rounded-full transition-transform duration-300 ease-in-out"
                            style="transform: translateX({voiceSpectrum === 'formal' ? '0' : 'calc(100% + 4px)'})"
                            aria-hidden="true"
                        ></div>
                        <button
                            type="button"
                            onclick={() => voiceSpectrum = 'formal'}
                            class="relative z-10 flex-1 flex items-center justify-center font-label-md text-label-md transition-colors {voiceSpectrum === 'formal' ? 'text-white' : 'text-on-surface-variant'}"
                            aria-pressed={voiceSpectrum === 'formal'}
                        >
                            Formal
                        </button>
                        <button
                            type="button"
                            onclick={() => voiceSpectrum = 'casual'}
                            class="relative z-10 flex-1 flex items-center justify-center font-label-md text-label-md transition-colors {voiceSpectrum === 'casual' ? 'text-white' : 'text-on-surface-variant'}"
                            aria-pressed={voiceSpectrum === 'casual'}
                        >
                            Casual
                        </button>
                    </div>
                </fieldset>

                <!-- Traits Grid -->
                <fieldset class="space-y-xs border-0 p-0 m-0">
                    <legend class="text-label-sm font-label-sm text-secondary px-1 mb-2 block w-full">Specific Traits</legend>
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-stack-gap">
                        {#each Object.entries(traits) as [key, value]}
                            <label class="flex items-center gap-sm p-3 border border-outline-variant rounded-lg cursor-pointer hover:bg-surface-container-low transition-colors active:scale-95">
                                <input
                                    type="checkbox"
                                    bind:checked={traits[key as keyof typeof traits]}
                                    class="w-4 h-4 rounded text-primary focus:ring-primary border-outline"
                                />
                                <span class="text-body-md font-medium text-on-surface capitalize">{key}</span>
                            </label>
                        {/each}
                    </div>
                </fieldset>
            </div>
        </section>

        <!-- Automated Stop-Rules Section -->
        <section class="space-y-sm">
            <h2 class="text-label-md font-label-md text-secondary uppercase tracking-wider px-1">Automated Stop-Rules</h2>

            <div class="bg-surface-container-lowest border border-outline-variant rounded-xl overflow-hidden divide-y divide-outline-variant">

                {#each stopRules as rule (rule.id)}
                    <div class="flex items-center justify-between p-md hover:bg-surface-container-low transition-colors">
                        <div class="space-y-xs">
                            <p class="text-body-md font-semibold text-on-surface">{rule.name}</p>
                            <p class="text-label-sm text-on-surface-variant">{rule.description}</p>
                        </div>
                        <label class="switch" aria-label={`Toggle ${rule.name}`}>
                            <input
                                type="checkbox"
                                bind:checked={rule.active}
                            />
                            <span class="slider" aria-hidden="true"></span>
                        </label>
                    </div>
                {/each}

                <!-- Add Custom Rule Button -->
                <button
                    type="button"
                    onclick={addCustomRule}
                    class="w-full flex items-center justify-center gap-2 p-md text-primary font-label-md text-label-md hover:bg-surface-container-high transition-all active:scale-95 group"
                >
                    <span class="material-symbols-outlined text-[20px] group-hover:rotate-90 transition-transform">add</span>
                    Add custom rule
                </button>
            </div>
        </section>

    </div>

    <!-- BottomNavBar -->
    <nav class="fixed bottom-0 w-full z-50 bg-surface border-t border-outline-variant shadow-sm flex justify-around items-center h-20 px-2 pb-safe">
        <!-- Agents (Inactive) -->
        <a href="#agents" class="flex flex-col items-center justify-center text-on-surface-variant px-4 py-1 hover:bg-surface-container-highest transition-colors duration-200">
            <span class="material-symbols-outlined">smart_toy</span>
            <span class="font-label-md text-label-md mt-1">Agents</span>
        </a>
        <!-- Chat (Inactive) -->
        <a href="#chat" class="flex flex-col items-center justify-center text-on-surface-variant px-4 py-1 hover:bg-surface-container-highest transition-colors duration-200">
            <span class="material-symbols-outlined">chat_bubble</span>
            <span class="font-label-md text-label-md mt-1">Chat</span>
        </a>
        <!-- History (Inactive) -->
        <a href="#history" class="flex flex-col items-center justify-center text-on-surface-variant px-4 py-1 hover:bg-surface-container-highest transition-colors duration-200">
            <span class="material-symbols-outlined">history</span>
            <span class="font-label-md text-label-md mt-1">History</span>
        </a>
        <!-- Settings (Active) -->
        <a href="#settings" class="flex flex-col items-center justify-center bg-secondary-container text-on-secondary-container rounded-full px-4 py-1 scale-95 transition-transform duration-150">
            <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' 1;">settings</span>
            <span class="font-label-md text-label-md mt-1">Settings</span>
        </a>
    </nav>
</main>
