#!/bin/bash
# Esegui questo script dalla cartella /Users/ali_claud/VoiceLog/
# comando: bash aggiorna_alidu_recal.sh

echo "🔄 Aggiornamento progetto a Alidu-Recal..."

# 1. Rinomina app_name in strings.xml
sed -i '' 's/VoiceLog/Alidu-Recal/g' app/src/main/res/values/strings.xml
echo "✅ strings.xml aggiornato"

# 2. Aggiorna namespace e applicationId in app/build.gradle
sed -i '' "s/namespace 'com.voicelog'/namespace 'com.alidurecal'/g" app/build.gradle
sed -i '' "s/applicationId \"com.voicelog\"/applicationId \"com.alidurecal\"/g" app/build.gradle
echo "✅ app/build.gradle aggiornato"

# 3. Aggiorna AndroidManifest.xml
sed -i '' 's/com.voicelog/com.alidurecal/g' app/src/main/AndroidManifest.xml
echo "✅ AndroidManifest.xml aggiornato"

# 4. Rinomina package Java
mkdir -p app/src/main/java/com/alidurecal
cp app/src/main/java/com/voicelog/MainActivity.kt app/src/main/java/com/alidurecal/MainActivity.kt
sed -i '' 's/package com.voicelog/package com.alidurecal/g' app/src/main/java/com/alidurecal/MainActivity.kt
echo "✅ Package rinominato in com.alidurecal"

# 5. Crea cartelle icone e copia le icone scaricate
for density in mdpi hdpi xhdpi xxhdpi xxxhdpi; do
    mkdir -p "app/src/main/res/mipmap-$density"
done
echo "✅ Cartelle mipmap create"

# 6. Aggiorna settings.gradle
sed -i '' "s/rootProject.name = \"VoiceLog\"/rootProject.name = \"AliduRecal\"/g" settings.gradle
echo "✅ settings.gradle aggiornato"

echo ""
echo "✅ FATTO! Ora copia le icone manualmente:"
echo "   - icons/mipmap-mdpi/ic_launcher.png → app/src/main/res/mipmap-mdpi/"
echo "   - icons/mipmap-hdpi/ic_launcher.png → app/src/main/res/mipmap-hdpi/"
echo "   - icons/mipmap-xhdpi/ic_launcher.png → app/src/main/res/mipmap-xhdpi/"
echo "   - icons/mipmap-xxhdpi/ic_launcher.png → app/src/main/res/mipmap-xxhdpi/"
echo "   - icons/mipmap-xxxhdpi/ic_launcher.png → app/src/main/res/mipmap-xxxhdpi/"
echo "   (fai lo stesso per ic_launcher_round.png)"
echo ""
echo "🚀 Poi fai push su GitHub e Actions compilerà l'APK!"
