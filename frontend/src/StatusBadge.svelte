<script>
  export let status = "";
  export let size = "sm"; // "sm" | "md"

  // Centralized Status Configuration for Design Tokens
  const statusConfigs = {
    ACTIVE: {
      label: 'AI Active',
      classes: 'bg-green-100 text-green-800 border border-green-300',
      icon: 'auto_awesome'
    },
    ESCALATED: {
      label: 'Human Intervention Required',
      classes: 'bg-yellow-100 text-yellow-800 border border-yellow-300',
      icon: 'warning'
    },
    PAUSED: {
      label: 'Paused AI',
      classes: 'bg-blue-50 text-blue-600 border border-blue-200',
      icon: ''
    },
    RESOLVED: {
      label: 'Closed/Converted',
      classes: 'bg-blue-100 text-blue-800 border border-blue-300',
      icon: ''
    },
    CLOSED: {
      label: 'Closed/Converted',
      classes: 'bg-blue-100 text-blue-800 border border-blue-300',
      icon: ''
    },
    CONVERTED: {
      label: 'Closed/Converted',
      classes: 'bg-blue-100 text-blue-800 border border-blue-300',
      icon: ''
    },
    SPAM: {
      label: 'Spam/Blocked',
      classes: 'bg-red-100 text-red-800 border border-red-300',
      icon: ''
    },
    BLOCKED: {
      label: 'Spam/Blocked',
      classes: 'bg-red-100 text-red-800 border border-red-300',
      icon: ''
    }
  };

  $: config = statusConfigs[(status || "").toUpperCase()] || {
    label: status || "Unknown",
    classes: 'bg-slate-100 text-slate-800 border border-slate-300',
    icon: ''
  };

  $: animateClass = (status && status.toUpperCase() === 'ESCALATED' && size === 'md') ? 'animate-pulse' : '';
</script>

<span class="{config.classes} font-bold rounded uppercase tracking-wider flex items-center gap-0.5 {size === 'md' ? 'px-2 py-0.5 text-[10px]' : 'px-1.5 py-0.5 text-[9px]'} {animateClass}">
  {#if config.icon}
    <span class="material-symbols-outlined {size === 'md' ? 'text-[11px]' : 'text-[10px]'}">{config.icon}</span>
  {/if}
  {config.label}
</span>
