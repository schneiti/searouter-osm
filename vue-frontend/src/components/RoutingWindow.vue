<template>
  <div>
    <b-card header="Routing Options">
      <table>
        <tr>
          <td>
            <b-input-group prepend="Start point" class="mt-3">
              <b-form-input
                v-model="startPoint"
                placeholder="Latitude (N) Longitude (E)"
              ></b-form-input>
              <b-input-group-append>
                <b-button
                  @click="choosePointOnMap(true)"
                  :disabled="chooseStartPointActive"
                  variant="info"
                  >Select on map</b-button
                >
              </b-input-group-append>
            </b-input-group>
          </td>

          <td>
            <b-input-group prepend="Target point" class="mt-3">
              <b-form-input
                v-model="targetPoint"
                placeholder="Latitude (N) Longitude (E)"
              ></b-form-input>
              <b-input-group-append>
                <b-button
                  @click="choosePointOnMap(false)"
                  :disabled="chooseTargetPointActive"
                  variant="info"
                  >Select on map</b-button
                >
              </b-input-group-append>
            </b-input-group>
          </td>
          <td>
            <b-input-group prepend="Router" class="mt-3">
            <b-form-select v-model="selectedRouter" :options="options"></b-form-select>
          </b-input-group>
          </td>
        </tr>
        <tr>
          <td>
            <b-button @click="calcRoute(startPoint, targetPoint)"
              >Calculate route</b-button
            >
          </td>
        </tr>
      </table>
    </b-card>
    <b-card header="World Map">
      <div id="map"></div>
    </b-card>
  </div>
</template>

<script>
import "leaflet/dist/leaflet.css";
import L from "leaflet";
import axios from "axios";

delete L.Icon.Default.prototype._getIconUrl;

L.Icon.Default.mergeOptions({
  iconRetinaUrl: require("leaflet/dist/images/marker-icon-2x.png"),
  iconUrl: require("leaflet/dist/images/marker-icon.png"),
  shadowUrl: require("leaflet/dist/images/marker-shadow.png"),
});

export default {
  name: "RoutingWindow",
  components: {},
  data() {
    return {
      chooseStartPointActive: false,
      chooseTargetPointActive: false,
      startPoint: null,
      startMarker: null,
      targetPoint: null,
      targetMarker: null,
      route: null,
      map: null,
      selectedRouter: null,
      options: [
          { value: 'dijkstra', text: 'Dijkstra' },
          { value: 'bidijkstra', text: 'Bidirectional Dijkstra' },
          { value: 'astar', text: 'A*' },
          { value: 'biastar_sym', text: 'Bidirectional A* (symmetric)' },
          { value: 'biastar_cons', text: 'Bidirectional A* (consistent)' },
          { value: 'alt_astar', text: 'ALT-A*' },
          { value: 'alt_biastar_sym', text: 'ALT-Bidirectional A* (symmetric)' },
          { value: 'alt_biastar_cons', text: 'ALT-Bidirectional A* (consistent)' },
        ]
    };
  },
  methods: {
    // Setup the leaflet map
    setupLeafletMap: function () {
      console.log("Setuup Leaflet");
      this.map = L.map("map").setView([20, 0], 3);
      L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        attribution:
          '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      }).addTo(this.map);

      this.map.on("click", (e) => {
        if (this.chooseStartPointActive) {
          this.startPoint = "" + e.latlng.lat + " " + e.latlng.lng;
          this.chooseStartPointActive = false;
        }

        if (this.chooseTargetPointActive) {
          this.targetPoint = "" + e.latlng.lat + " " + e.latlng.lng;
          this.chooseTargetPointActive = false;
        }
      });

      this.markers = [];
    },

    // Let the user choose a point on the map by clicking on it
    choosePointOnMap(isStart) {
      console.log("It works");
      if (isStart == true) {
        this.chooseStartPointActive = true;
        this.chooseTargetPointActive = false;
      } else {
        this.chooseStartPointActive = false;
        this.chooseTargetPointActive = true;
      }
    },

    sanitizeLongitude: function (longitude) {
      if (longitude < -180) {
        return longitude % 180;
      } else if (longitude > 180) {
        return -180 + (longitude % 180);
      }
      return longitude;
    },

    // Calculate the route
    calcRoute(startPoint, targetPoint) {
      var latlong = startPoint.split(" ");
      var latOne = parseFloat(latlong[0]);
      var longOne = this.sanitizeLongitude(latlong[1]);

      latlong = targetPoint.split(" ");
      var latTwo = parseFloat(latlong[0]);
      var longTwo = this.sanitizeLongitude(parseFloat(latlong[1]));

      let reqObj = {
        startPoint: {
          latitude: latOne,
          longitude: longOne,
        },
        endPoint: {
          latitude: latTwo,
          longitude: longTwo,
        },
        router: this.selectedRouter,
      };

      axios
        .post("http://localhost:8080/route", reqObj)
        .then((response) => {
          this.responseData = response.data;

      if (response.data.routeFound === true) {
        // This is what gets displayed in the browser when the result is finished
        alert(
        "Distance of route (m): " + response.data.overallDistance + "\nCalc time (ms): " + response.data.calculationTimeInMs
        );
      } else {
        alert(
        "No route found" + "\nCalc time (ms): " + response.data.calculationTimeInMs
        );
        return;
      }

          this.startPoint = response.data.pathCoordinates[0][0] + " " + response.data.pathCoordinates[0][1];
          console.log(this.startPoint);
          this.targetPoint = response.data.pathCoordinates[response.data.pathCoordinates.length-1][0] + " " + response.data.pathCoordinates[response.data.pathCoordinates.length-1][1];
          console.log(this.targetPoint);

          this.addRoute(response.data.pathCoordinates, response.data.overallDistance);
        })
        .catch((error) => {
          alert(error.response.data);
        });
    },

    addRoute(latLongsArray, distance) {
      if (this.route != null) {
        this.route.remove();
      }

      this.route = L.polyline(latLongsArray, { color: "red" })
        .bindPopup("Distance: " + distance + "m")
        .addTo(this.map);
    },
  },
  watch: {
    startPoint: function (val) {
      if (!val) {
        return;
      }
      var latlong = val.split(" ");
      var lat = parseFloat(latlong[0]);
      var long = parseFloat(latlong[1]);

      if (this.startMarker != null) {
        this.startMarker.remove();
      }
      this.startMarker = L.marker([lat, long])
        .bindTooltip("Start", {
          permanent: true,
          direction: "right",
        })
        .addTo(this.map);
    },
    targetPoint: function (val) {
      if (!val) {
        return;
      }
      var latlong = val.split(" ");
      var lat = parseFloat(latlong[0]);
      var long = parseFloat(latlong[1]);

      if (this.targetMarker != null) {
        this.targetMarker.remove();
      }
      this.targetMarker = L.marker([lat, long])
        .bindTooltip("Target", {
          permanent: true,
          direction: "right",
        })
        .addTo(this.map);
    },
  },
  mounted() {
    // This will be called every time this component will be called
    console.log("mounted");
    this.setupLeafletMap();
  },
};
</script>

<style>
#map {
  height: 600px;
}

.card {
  margin-left: 50px;
  margin-top: 30px;
  margin-right: 50px;
}

td {
  padding-top: 10px;
  padding-right: 10px;
}
</style>
