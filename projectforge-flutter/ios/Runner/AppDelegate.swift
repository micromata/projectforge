import Flutter
import PassKit
import UIKit
import UserNotifications

@main
@objc class AppDelegate: FlutterAppDelegate, FlutterImplicitEngineDelegate {
  /// To the Dart side: the push token, a tapped push, a push received while the app is open.
  private var pushChannel: FlutterMethodChannel?

  /// The link of the push that started the app, until Dart asks for it.
  private var launchLink: String?

  /// True once Dart has called `start` and takes links itself.
  private var pushStarted = false

  override func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
  ) -> Bool {
    return super.application(application, didFinishLaunchingWithOptions: launchOptions)
  }

  func didInitializeImplicitFlutterEngine(_ engineBridge: FlutterImplicitEngineBridge) {
    GeneratedPluginRegistrant.register(with: engineBridge.pluginRegistry)
    if let registrar = engineBridge.pluginRegistry.registrar(forPlugin: "ProjectForgePush") {
      registerPush(registrar.messenger())
    }
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

  private func registerPush(_ messenger: FlutterBinaryMessenger) {
    let channel = FlutterMethodChannel(name: "projectforge/push", binaryMessenger: messenger)
    pushChannel = channel
    channel.setMethodCallHandler { [weak self] call, result in
      switch call.method {
      case "start":
        self?.startPush(result)
      case "setBadge":
        let count = call.arguments as? Int ?? 0
        if #available(iOS 16.0, *) {
          UNUserNotificationCenter.current().setBadgeCount(count)
        } else {
          UIApplication.shared.applicationIconBadgeNumber = count
        }
        result(nil)
      default:
        result(FlutterMethodNotImplemented)
      }
    }
  }

  /// Asks for the permission (the system asks once) and registers for remote notifications. The token comes
  /// to `didRegisterForRemoteNotificationsWithDeviceToken`. Answers whether pushes are allowed and the link of
  /// the push that started the app.
  private func startPush(_ result: @escaping FlutterResult) {
    let center = UNUserNotificationCenter.current()
    center.delegate = self
    center.requestAuthorization(options: [.alert, .badge, .sound]) { [weak self] granted, _ in
      DispatchQueue.main.async {
        if granted {
          UIApplication.shared.registerForRemoteNotifications()
        }
        let link = self?.launchLink
        self?.launchLink = nil
        self?.pushStarted = true
        result(["granted": granted, "launchLink": link as Any, "environment": self?.apsEnvironment() as Any])
      }
    }
  }

  /// `SANDBOX` or `PRODUCTION`, as the app is signed: read from the provisioning profile (`aps-environment`), which
  /// a build started from Xcode has even in release mode. Builds from the store or TestFlight have none: production.
  private func apsEnvironment() -> String? {
    guard let url = Bundle.main.url(forResource: "embedded", withExtension: "mobileprovision"),
      let data = try? Data(contentsOf: url),
      let text = String(data: data, encoding: .isoLatin1),
      let key = text.range(of: "<key>aps-environment</key>"),
      let open = text.range(of: "<string>", range: key.upperBound..<text.endIndex),
      let close = text.range(of: "</string>", range: open.upperBound..<text.endIndex)
    else {
      return nil
    }
    return text[open.upperBound..<close.lowerBound] == "development" ? "SANDBOX" : "PRODUCTION"
  }

  override func application(
    _ application: UIApplication,
    didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
  ) {
    super.application(application, didRegisterForRemoteNotificationsWithDeviceToken: deviceToken)
    let token = deviceToken.map { String(format: "%02x", $0) }.joined()
    pushChannel?.invokeMethod("token", arguments: token)
  }

  override func application(
    _ application: UIApplication,
    didFailToRegisterForRemoteNotificationsWithError error: Error
  ) {
    super.application(application, didFailToRegisterForRemoteNotificationsWithError: error)
  }

  /// A push while the app is open: shown as a banner, the app loads the notifications.
  override func userNotificationCenter(
    _ center: UNUserNotificationCenter,
    willPresent notification: UNNotification,
    withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
  ) {
    pushChannel?.invokeMethod("received", arguments: nil)
    completionHandler([.banner, .sound])
  }

  /// The user tapped a push: its link is opened, now or as soon as the app has started.
  override func userNotificationCenter(
    _ center: UNUserNotificationCenter,
    didReceive response: UNNotificationResponse,
    withCompletionHandler completionHandler: @escaping () -> Void
  ) {
    let link = response.notification.request.content.userInfo["link"] as? String ?? ""
    if pushStarted {
      pushChannel?.invokeMethod("link", arguments: link)
    } else {
      launchLink = link
    }
    completionHandler()
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
