# labs-101

## Build
Build the App.
```sh
cd ios
xcodebuild -project ios.xcodeproj -scheme ios \
  -destination 'platform=iOS Simulator,OS=26.5,name=iPhone 17 Pro' \
  -configuration Debug -derivedDataPath ./build/labs101-dd build
```

Install the App on an Iphone connected to the Mac.
```sh
cd labs-101
xcodebuild -project labs-101.xcodeproj -scheme labs-101 \
  -destination 'platform=iOS,id=00008110-001228102686601E' \
  -configuration Debug -derivedDataPath ./build/labs101-dev \
  -allowProvisioningUpdates build
xcrun devicectl device install app \
  --device 5FDC10C4-1F57-56AC-B5A5-3CD2ADDE6566 \
  ./build/labs101-dev/Build/Products/Debug-iphoneos/labs-101.app
```