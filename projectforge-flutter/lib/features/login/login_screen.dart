/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api/api_exception.dart';
import '../../core/auth/app_login.dart';
import '../../core/auth/auth_controller.dart';
import '../../core/auth/session.dart';
import '../../core/i18n/translations.dart';
import '../../core/log/log_screen.dart';

/// The server url and the button opening the login of that server in the browser (password and second factor
/// are entered there, see AppLogin).
class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key, this.messageKey, this.addServer = false});

  /// Why the app was logged out (an i18n key), if it was.
  final String? messageKey;

  /// Shown over the app to log in at another server (the current login stays stored): with a way back, closed by the
  /// login.
  final bool addServer;

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  static const _devServerUrl = String.fromEnvironment('PF_SERVER_URL');

  final _server = TextEditingController();
  bool _busy = false;
  String? _error;

  /// The servers of the last logins, most recent first.
  List<String> _servers = const [];

  /// The stored logins by server: a tap on such a server switches to it without the browser.
  Map<String, Session> _sessions = const {};

  @override
  void initState() {
    super.initState();
    _loadServers().then((_) {
      // The last server, otherwise the development server of `bin/pfApp.sh run <slot>` (only set by it).
      final preset = _servers.firstOrNull ?? _devServerUrl;
      if (preset.isNotEmpty && mounted && _server.text.isEmpty) {
        _server.text = preset;
      }
    });
  }

  Future<void> _loadServers() async {
    final auth = ref.read(authControllerProvider.notifier);
    final servers = await auth.servers();
    final sessions = await auth.sessions();
    if (mounted) {
      setState(() {
        _servers = servers;
        _sessions = {for (final it in sessions) it.serverUrl: it};
      });
    }
  }

  Future<void> _removeServer(String serverUrl) async {
    await ref.read(authControllerProvider.notifier).removeServer(serverUrl);
    await _loadServers();
  }

  /// A server with a stored login is switched to at once, any other one only put into the field.
  Future<void> _tap(String serverUrl) async {
    final stored = _sessions[serverUrl];
    if (stored == null) return _select(serverUrl);
    await ref.read(authControllerProvider.notifier).switchTo(stored);
    if (widget.addServer && mounted) Navigator.of(context).pop();
  }

  void _select(String serverUrl) {
    setState(() {
      _server.text = serverUrl;
      _error = null;
    });
  }

  @override
  void dispose() {
    _server.dispose();
    super.dispose();
  }

  Future<void> _login() async {
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await ref.read(authControllerProvider.notifier).login(_server.text);
      if (widget.addServer && mounted) Navigator.of(context).pop();
    } on AppLoginCancelled {
      // The user closed the browser: nothing to report.
    } on InvalidServerUrl {
      setState(() => _error = context.t('app.server.invalid'));
    } on InsecureServerUrl {
      setState(() => _error = context.t('app.server.httpsRequired'));
    } on ApiException catch (e) {
      setState(
        () => _error = e.isOffline
            ? context.t('app.server.unreachable')
            : '${context.t('app.login.failed')} (${e.message})',
      );
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final message = widget.messageKey;
    // The development server is offered, but only stored by a login.
    final devServer =
        _devServerUrl.isNotEmpty && !_servers.contains(_devServerUrl)
        ? _devServerUrl
        : null;
    return Scaffold(
      appBar: widget.addServer ? AppBar() : null,
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text(
                    'ProjectForge',
                    style: Theme.of(context).textTheme.headlineMedium,
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 32),
                  if (message != null) ...[
                    Text(
                      context.t(message),
                      style: TextStyle(
                        color: Theme.of(context).colorScheme.error,
                      ),
                    ),
                    const SizedBox(height: 16),
                  ],
                  TextField(
                    controller: _server,
                    keyboardType: TextInputType.url,
                    autocorrect: false,
                    textInputAction: TextInputAction.go,
                    onSubmitted: (_) => _busy ? null : _login(),
                    decoration: InputDecoration(
                      labelText: context.t('app.server.label'),
                      hintText: 'projectforge.example.com',
                      errorText: _error,
                      errorMaxLines: 3,
                    ),
                  ),
                  if (_servers.isNotEmpty || devServer != null) ...[
                    const SizedBox(height: 16),
                    Text(
                      context.t('app.server.recent'),
                      style: Theme.of(context).textTheme.labelMedium,
                    ),
                    for (final server in _servers)
                      _ServerTile(
                        serverUrl: server,
                        signedIn: _sessions.containsKey(server),
                        onTap: _busy ? null : () => _tap(server),
                        onRemove: _busy ? null : () => _removeServer(server),
                      ),
                    if (devServer != null)
                      _ServerTile(
                        serverUrl: devServer,
                        onTap: _busy ? null : () => _select(devServer),
                      ),
                  ],
                  const SizedBox(height: 16),
                  Text(
                    context.t('app.login.info'),
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                  const SizedBox(height: 24),
                  FilledButton(
                    onPressed: _busy ? null : _login,
                    child: _busy
                        ? const SizedBox(
                            height: 20,
                            width: 20,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : Text(context.t('login')),
                  ),
                  const SizedBox(height: 8),
                  // What happened in a failed login, e.g. behind an SSO proxy.
                  TextButton(
                    onPressed: () => Navigator.of(context).push(
                      MaterialPageRoute<void>(
                        builder: (_) => const LogScreen(),
                      ),
                    ),
                    child: Text(context.t('app.log.title')),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

/// A server of the last logins: a tap puts it into the field, [onRemove] (if given) removes it from the list.
class _ServerTile extends StatelessWidget {
  const _ServerTile({
    required this.serverUrl,
    this.signedIn = false,
    this.onTap,
    this.onRemove,
  });

  final String serverUrl;

  /// There is a stored login at the server.
  final bool signedIn;
  final VoidCallback? onTap;
  final VoidCallback? onRemove;

  @override
  Widget build(BuildContext context) => ListTile(
    dense: true,
    contentPadding: EdgeInsets.zero,
    leading: const Icon(Icons.dns_outlined),
    title: Text(serverUrl),
    subtitle: signedIn ? Text(context.t('app.server.loggedIn')) : null,
    onTap: onTap,
    trailing: onRemove == null
        ? null
        : IconButton(
            icon: const Icon(Icons.close),
            tooltip: context.t('app.server.remove'),
            onPressed: onRemove,
          ),
  );
}
