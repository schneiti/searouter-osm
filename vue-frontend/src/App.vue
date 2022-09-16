<template>
  <div id="app">
    <Navbar/>
    <RoutingWindow/>
  </div>
</template>

<script>
import Navbar from "./components/Navbar.vue";
import RoutingWindow from "./components/RoutingWindow.vue";

export default {
  name: "App",
  components: {
    Navbar,
    RoutingWindow,
  },
 
};
</script>

<style>

</style>
