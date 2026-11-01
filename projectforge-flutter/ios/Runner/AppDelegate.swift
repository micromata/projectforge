import Flutter
import PassKit
import UIKit

@main
@objc class AppDelegate: FlutterAppDelegate, FlutterImplicitEngineDelegate {
  override func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
  ) -> Bool {
    return super.application(application, didFinishLaunchingWithOptions: launchOptions)
  }

  func didInitializeImplicitFlutterEngine(_ engineBridge: FlutterImplicitEngineBridge) {
    GeneratedPluginRegistrant.register(with: engineBridge.pluginRegistry)
    if let registrar = engineBridge.pluginRegistry.registrar(forPlugin: "ProjectForgeWallet") {
      let channel = FlutterMethodChannel(
        name: "projectforge/wallet", binaryMessenger: registrar.messenger())
      channel.setMethodCallHandler { [weak self] call, result in
        guard call.method == "addPass", let data = call.arguments as? FlutterStandardTypedData else {
          result(FlutterMethodNotImplemented)
          return
        }
        self?.addPass(data.data, result: result)
      }
    }
  }

  /// Shows the system sheet to add the pass (`.pkpass`) to the Wallet.
  private func addPass(_ data: Data, result: @escaping FlutterResult) {
    guard PKAddPassesViewController.canAddPasses() else {
      result(FlutterError(code: "unavailable", message: "Wallet is not available.", details: nil))
      return
    }
    do {
      let pass = try PKPass(data: data)
      if PKPassLibrary().containsPass(pass) {
        result(FlutterError(code: "exists", message: "The pass is already in the Wallet.", details: nil))
        return
      }
      guard let controller = PKAddPassesViewController(pass: pass),
        let root = UIApplication.shared.connectedScenes
          .compactMap({ ($0 as? UIWindowScene)?.keyWindow?.rootViewController }).first
      else {
        result(FlutterError(code: "failed", message: "The pass can't be shown.", details: nil))
        return
      }
      var top = root
      while let presented = top.presentedViewController { top = presented }
      top.present(controller, animated: true)
      result(nil)
    } catch {
      result(FlutterError(code: "invalid", message: error.localizedDescription, details: nil))
    }
  }
}
