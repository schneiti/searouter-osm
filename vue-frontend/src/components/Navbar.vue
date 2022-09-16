<template>
  <b-navbar type="dark" variant="dark">

      <b-navbar-brand href="#">SeaRouter</b-navbar-brand>

  </b-navbar>
</template>

<script>
export default {
  name: "Navbar",
  data() {
    return {
      timestamp: "",
    };
  },
  methods: {
    getNow: function () {
      const today = new Date();
      const date =
        today.getFullYear() +
        "-" +
        (today.getMonth() + 1) +
        "-" +
        today.getDate();
      const time =
        today.getHours() + ":" + today.getMinutes() + ":" + today.getSeconds();
      const dateTime = date + " " + time;
      this.timestamp = dateTime;
    },
  },
  created() {
    setInterval(this.getNow, 1000);
  },
};
</script>
