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

import 'dart:async';

import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../api/api_exception.dart';
import '../auth/auth_controller.dart';
import 'user_status.dart';
import 'user_status_repository.dart';

final userStatusRepositoryProvider = Provider<UserStatusRepository?>((ref) {
  final api = ref.watch(apiClientProvider);
  return api == null ? null : UserStatusRepository(api);
});

final userStatusProvider =
    AsyncNotifierProvider<UserStatusController, UserStatus>(
      UserStatusController.new,
    );

/// The user status for the badge of the chat tab and the bell of the notifications, reloaded every [interval] while the app is in the
/// foreground, on resuming it and on [refresh] (e.g. leaving the chat tab) and when a push arrives.
///
/// The app token has no session, so the polling keeps nothing alive; the server caches the count for some seconds.
class UserStatusController extends AsyncNotifier<UserStatus> {
  static const interval = Duration(seconds: 60);

  Timer? _timer;

  @override
  Future<UserStatus> build() {
    final repository = ref.watch(userStatusRepositoryProvider)!;
    final lifecycle = AppLifecycleListener(
      onResume: () {
        refresh();
        _startTimer();
      },
      onPause: _stopTimer,
    );
    ref.onDispose(() {
      _stopTimer();
      lifecycle.dispose();
    });
    _startTimer();
    return repository.load();
  }

  void _startTimer() {
    _stopTimer();
    _timer = Timer.periodic(interval, (_) => refresh());
  }

  void _stopTimer() {
    _timer?.cancel();
    _timer = null;
  }

  /// Loads the status again; offline (or on an error) the last one stays.
  Future<void> refresh() async {
    final repository = ref.read(userStatusRepositoryProvider);
    if (repository == null) return;
    try {
      final status = await repository.load();
      if (ref.mounted) state = AsyncData(status);
    } on ApiException catch (e, stack) {
      if (ref.mounted && !state.hasValue) state = AsyncError(e, stack);
    }
  }
}
