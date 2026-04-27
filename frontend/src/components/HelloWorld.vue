<script setup>
defineProps({
  msg: {
    type: String,
    required: true,
  },
})
</script>

<template>
  <div class="card">
    <h2>{{ msg }}</h2>
    <p>You can start building pages, API calls, and metric dashboards from here.</p>
  </div>
</template>
