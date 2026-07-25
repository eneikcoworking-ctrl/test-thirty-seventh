<script>
  export let status = "";
  export let size = "sm"; // "sm" | "md"

  // Centralized Status Configuration for Design Tokens
  const statusConfigs = {
    ACTIVE: {
      label: 'AI Active',
      classes: 'bg-emerald-100 text-emerald-800 border border-emerald-300',
      icon: 'smart_toy'
    },
    ESCALATED: {
      label: 'Human Intervention Required',
      classes: 'bg-amber-100 text-amber-800 border border-amber-300',
      icon: 'warning'
    },
    PAUSED: {
      label: 'Spam/Blocked',
      classes: 'bg-rose-100 text-rose-800 border border-rose-300',
      icon: 'block'
    },
    RESOLVED: {
      label: 'Closed/Converted',
      classes: 'bg-blue-100 text-blue-800 border border-blue-300',
      icon: 'check_circle'
    },
    CLOSED: {
      label: 'Closed/Converted',
      classes: 'bg-blue-100 text-blue-800 border border-blue-300',
      icon: 'check_circle'
    },
    CONVERTED: {
      label: 'Closed/Converted',
      classes: 'bg-blue-100 text-blue-800 border border-blue-300',
      icon: 'check_circle'
    },
    SPAM: {
      label: 'Spam/Blocked',
      classes: 'bg-rose-100 text-rose-800 border border-rose-300',
      icon: 'block'
    },
    BLOCKED: {
      label: 'Spam/Blocked',
      classes: 'bg-rose-100 text-rose-800 border border-rose-300',
      icon: 'block'
    }
  };

  $: config = statusConfigs[(status || "").toUpperCase()] || {
    label: status || "Unknown",
    classes: 'bg-slate-100 text-slate-800 border border-slate-300',
    icon: ''
  };

  $: animateClass = (status && status.toUpperCase() === 'ESCALATED' && size === 'md') ? 'animate-pulse' : '';
</script>

<span class="{config.classes} font-bold rounded uppercase tracking-wider flex items-center gap-0.5 {size === 'md' ? 'px-2 py-0.5 text-xs' : 'px-1.5 py-0.5 text-xs'} {animateClass}">
  {#if config.icon}
    <span class="material-symbols-outlined filled-icon text-xs">{config.icon}</span>
  {/if}
  {config.label}
</span>

<style>
  /* Clean CSS class for Material Symbols to completely eliminate inline styles */
  .filled-icon {
    font-variation-settings: 'FILL' 1;
  }
</style>
