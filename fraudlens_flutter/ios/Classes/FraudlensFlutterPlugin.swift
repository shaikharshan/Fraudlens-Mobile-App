import Flutter
import UIKit

/// iOS does not ship the Kotlin FraudLens SDK. All methods return `notImplemented` unless you add Swift/native bindings.
public class FraudlensFlutterPlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "fraudlens_flutter", binaryMessenger: registrar.messenger())
    let instance = FraudlensFlutterPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    result(FlutterError(
      code: "UNIMPLEMENTED",
      message: "FraudLens native SDK is Android-only in this package. Use REST from Dart on iOS or extend this plugin.",
      details: nil
    ))
  }
}
