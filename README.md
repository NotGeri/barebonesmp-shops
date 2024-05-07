# Shops
A simple [PaperMC plugin](https://papermc.io) for Minecraft to track in-game player shops' stock and list them
online using a super basic [Vue](https://vuejs.org) frontend and eventually an
in-game [Fabric mod](https://fabricmc.io).

## Installing
First, download the source code or use `git clone git@github.com:NotGeri/shops.git`

### Plugin
1. Ensure you have Java 17+ and [Gradle](https://gradle.org/install/) installed.
2. In the main project directory, use the `gradle plugin:build` command (or `gradlew plugin:build`) if you are using Windows
3. You can find the compiled JAR In the `plugin/build/libs/` directory.
4. Upload the JAR into your server's `plugins` folder. 
5. Restart the server to apply the change. This will generate the default configuration in `plugins/Shops/config.yml`
6. Ensure the `api.port` setting uses a TCP port that will be available on your system. 
  Make sure to restart the server if you change this setting.
7. Set up a DNS record and using a reverse-proxy, ensure the HTTP can be reached via HTTPS.

### Frontend
1. Ensure you have [Node.js](https://nodejs.org/en) 18+ installed.
2. Open the `frontend` folder.
3. Install the dependencies using `npm i`
4. Adjust the endpoint in `.env` to match with the reverse-proxied link from step #7
5. Build for production using `npm build`
