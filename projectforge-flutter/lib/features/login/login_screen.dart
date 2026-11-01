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
import '../../core/i18n/translations.dart';

/// The server url and the button opening the login of that server in the browser (password and second factor
/// are entered there, see AppLogin).
class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key, this.messageKey});

  /// Why the app was logged out (an i18n key), if it was.
  final String? messageKey;

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  static const _devServerUrl = String.fromEnvironment('PF_SERVER_URL');

  final _server = TextEditingController();
  bool _busy = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    ref.read(authControllerProvider.notifier).lastServerUrl().then((url) {
      // The development server of `bin/pfApp.sh run <slot>` (only set by it), otherwise the last server.
      final preset = _devServerUrl.isNotEmpty ? _devServerUrl : url;
      if (preset != null && mounted && _server.text.isEmpty) {
        _server.text = preset;
      }
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
    } on AppLoginCancelled {
      // The user closed the browser: nothing to report.
    } on InvalidServerUrl {
      setState(() => _error = context.t('app.server.invalid'));
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
    return Scaffold(
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
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
