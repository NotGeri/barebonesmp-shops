#!/usr/bin/env bash
client_jar=https://piston-data.mojang.com/v1/objects/fd19469fed4a4b4c15b2d5133985f0e3e7816a8a/client.jar

rm -rf build/
rm -rf src/assets/items/

mkdir -p build/
cd build/ || {
  ecoh 'Unable to open build directory'
  exit 1
}

# Download the JAR
if ! wget -O client.jar "$client_jar"; then
  echo 'Unable to WGET client JAR'
fi

# Extract it
unzip client.jar

# Generate translations
npm i
npx tsx ../translations.ts
mv translations.json ../src/assets/

# Clone RC
if ! git clone https://github.com/aternosorg/renderchest.git; then
  echo 'Unable to clone renderchest'
  exit 1
fi

cd renderchest/ || {
  echo 'Unable to open renderchest directory'
  exit 1
}

# Install dependencies
composer install

# Run it
./renderchest --assets ../assets --output out/ --namespace minecraft
mv out/items/minecraft/ ../../public/items/
