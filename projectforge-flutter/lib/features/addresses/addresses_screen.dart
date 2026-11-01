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

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api/api_exception.dart';
import '../../core/i18n/translations.dart';
import '../../core/widgets/error_view.dart';
import 'address.dart';
import 'address_avatar.dart';
import 'address_detail_screen.dart';
import 'addresses_controller.dart';

/// Loads the changes from the server; a failure is reported in a snackbar, the shown addresses stay.
Future<void> refreshAddresses(BuildContext context, WidgetRef ref) async {
  final messenger = ScaffoldMessenger.of(context);
  final ApiException? error = await ref
      .read(addressesProvider.notifier)
      .refresh();
  if (error == null || !context.mounted) return;
  messenger.showSnackBar(SnackBar(content: Text(context.errorText(error))));
}

/// The address book like the one of Apple: sections by the first letter with an index at the side, a search field,
/// and the address with its numbers and mail addresses on tap. Editing is done in the web pages ([onEdit], [onCreate]).
class AddressesScreen extends ConsumerStatefulWidget {
  const AddressesScreen({
    super.key,
    required this.onEdit,
    required this.onCreate,
    required this.active,
  });

  /// Shows the form of the address with this id.
  final void Function(int id) onEdit;

  /// Shows the form for a new address.
  final VoidCallback onCreate;

  /// The tab is shown: the addresses are synced silently when it is opened.
  final bool active;

  @override
  ConsumerState<AddressesScreen> createState() => _AddressesScreenState();
}

class _AddressesScreenState extends ConsumerState<AddressesScreen> {
  static const _rowHeight = 64.0;
  static const _headerHeight = 32.0;
  static const _indexMinimum = 20;

  final _scroll = ScrollController();
  final _search = TextEditingController();
  String _query = '';
  bool _favoritesOnly = false;

  @override
  void didUpdateWidget(AddressesScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.active && !oldWidget.active) {
      ref.read(addressesProvider.notifier).refreshIfStale();
    }
  }

  @override
  void dispose() {
    _scroll.dispose();
    _search.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final book = ref.watch(addressesProvider);
    return Scaffold(
      body: book.when(
        skipLoadingOnRefresh: true,
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => ErrorView(
          message: context.errorText(error),
          onRetry: () => ref.read(addressesProvider.notifier).refresh(),
        ),
        data: _content,
      ),
    );
  }

  Widget _content(AddressBook book) {
    final addresses = [
      for (final it in _favoritesOnly ? book.favorites : book.addresses)
        if (it.matches(_query)) it,
    ];
    final items = _items(addresses);
    final searching = _query.trim().isNotEmpty;
    final showIndex = !searching && addresses.length >= _indexMinimum;
    return Column(
      children: [
        if (book.fromCache)
          Padding(
            padding: const EdgeInsets.all(8),
            child: Text(
              context.t('app.address.offline'),
              style: TextStyle(color: Theme.of(context).colorScheme.error),
            ),
          ),
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 4, 16, 4),
          child: SegmentedButton<bool>(
            showSelectedIcon: false,
            segments: [
              ButtonSegment(
                value: true,
                icon: const Icon(Icons.star_outline),
                label: Text(context.t('app.address.favorites')),
              ),
              ButtonSegment(
                value: false,
                label: Text(context.t('app.address.all')),
              ),
            ],
            selected: {_favoritesOnly},
            onSelectionChanged: (selection) =>
                setState(() => _favoritesOnly = selection.first),
          ),
        ),
        Expanded(
          child: Stack(
            children: [
              RefreshIndicator(
                onRefresh: () => refreshAddresses(context, ref),
                child: items.isEmpty
                    ? ListView(
                        children: [
                          Padding(
                            padding: const EdgeInsets.all(32),
                            child: Text(
                              context.t(
                                searching
                                    ? 'app.address.noMatch'
                                    : _favoritesOnly
                                    ? 'app.address.noFavorites'
                                    : 'app.address.none',
                              ),
                              textAlign: TextAlign.center,
                            ),
                          ),
                        ],
                      )
                    : ListView.builder(
                        controller: _scroll,
                        padding: EdgeInsets.only(right: showIndex ? 20 : 0),
                        itemCount: items.length,
                        itemExtentBuilder: (index, _) =>
                            items[index] is String ? _headerHeight : _rowHeight,
                        itemBuilder: (context, index) {
                          final item = items[index];
                          return item is Address
                              ? _AddressRow(
                                  address: item,
                                  favorite: book.favoriteIds.contains(item.id),
                                  onTap: () => _open(item, book.fromCache),
                                )
                              : _SectionHeader(letter: item as String);
                        },
                      ),
              ),
              if (showIndex)
                Positioned(
                  right: 0,
                  top: 0,
                  bottom: 0,
                  width: 24,
                  child: _IndexBar(
                    existing: {for (final it in addresses) it.sectionLetter},
                    onLetter: (letter) => _jumpTo(items, letter),
                  ),
                ),
            ],
          ),
        ),
        _searchField(),
      ],
    );
  }

  /// The letter of each section (a String) followed by its addresses.
  List<Object> _items(List<Address> addresses) {
    final items = <Object>[];
    String? letter;
    for (final it in addresses) {
      if (it.sectionLetter != letter) {
        letter = it.sectionLetter;
        items.add(letter);
      }
      items.add(it);
    }
    return items;
  }

  /// "#" comes after "Z".
  static int _rank(String letter) =>
      letter == '#' ? 1000 : letter.codeUnitAt(0);

  void _jumpTo(List<Object> items, String letter) {
    var offset = 0.0;
    for (final item in items) {
      if (item is String && _rank(item) >= _rank(letter)) break;
      offset += item is String ? _headerHeight : _rowHeight;
    }
    if (!_scroll.hasClients) return;
    final max = _scroll.position.maxScrollExtent;
    _scroll.jumpTo(offset.clamp(0, max));
  }

  Widget _searchField() => SafeArea(
    top: false,
    child: Padding(
      padding: const EdgeInsets.fromLTRB(12, 4, 12, 8),
      child: Row(
        children: [
          Expanded(
            child: SearchBar(
              controller: _search,
              hintText: context.t('search'),
              leading: const Icon(Icons.search),
              elevation: const WidgetStatePropertyAll(0),
              constraints: const BoxConstraints(minHeight: 44),
              trailing: [
                if (_query.isNotEmpty)
                  IconButton(
                    icon: const Icon(Icons.clear),
                    onPressed: () {
                      _search.clear();
                      setState(() => _query = '');
                    },
                  ),
              ],
              onChanged: (value) => setState(() => _query = value),
            ),
          ),
          IconButton(
            tooltip: context.t('app.address.new'),
            icon: const Icon(Icons.add),
            onPressed: widget.onCreate,
          ),
        ],
      ),
    ),
  );

  void _open(Address address, bool offline) {
    FocusScope.of(context).unfocus();
    Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => AddressDetailScreen(
          address: address,
          // Editing happens on the server's pages.
          onEdit: offline ? null : () => widget.onEdit(address.id),
        ),
      ),
    );
  }
}

class _SectionHeader extends StatelessWidget {
  const _SectionHeader({required this.letter});

  final String letter;

  @override
  Widget build(BuildContext context) => Container(
    alignment: Alignment.bottomLeft,
    padding: const EdgeInsets.fromLTRB(16, 0, 16, 4),
    child: Text(
      letter,
      style: TextStyle(
        color: Theme.of(context).colorScheme.onSurfaceVariant,
        fontWeight: FontWeight.w500,
      ),
    ),
  );
}

class _AddressRow extends StatelessWidget {
  const _AddressRow({
    required this.address,
    required this.favorite,
    required this.onTap,
  });

  final Address address;
  final bool favorite;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final subtitle = address.subtitle;
    final name = address.listName;
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16),
        child: Row(
          children: [
            AddressAvatar(address: address, size: 44),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    name.isEmpty ? '–' : name,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  if (subtitle != null)
                    Text(
                      subtitle,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        color: Theme.of(context).colorScheme.onSurfaceVariant,
                      ),
                    ),
                ],
              ),
            ),
            if (favorite)
              Padding(
                padding: const EdgeInsets.only(left: 8),
                child: Icon(Icons.star, size: 18, color: Colors.amber.shade700),
              ),
          ],
        ),
      ),
    );
  }
}

/// A to Z and "#" at the side: touching or dragging jumps to the section.
class _IndexBar extends StatefulWidget {
  const _IndexBar({required this.existing, required this.onLetter});

  final Set<String> existing;
  final void Function(String letter) onLetter;

  @override
  State<_IndexBar> createState() => _IndexBarState();
}

class _IndexBarState extends State<_IndexBar> {
  static final _letters = [
    for (var c = 65; c <= 90; c++) String.fromCharCode(c),
    '#',
  ];

  String? _touched;

  @override
  Widget build(BuildContext context) {
    final color = Theme.of(context).colorScheme.primary;
    return LayoutBuilder(
      builder: (context, constraints) {
        final cell = constraints.maxHeight / _letters.length;
        void touch(double dy) {
          final index = (dy / cell).floor().clamp(0, _letters.length - 1);
          final letter = _letters[index];
          if (letter == _touched) return;
          _touched = letter;
          HapticFeedback.selectionClick();
          widget.onLetter(letter);
          setState(() {});
        }

        return GestureDetector(
          behavior: HitTestBehavior.opaque,
          onTapDown: (d) => touch(d.localPosition.dy),
          onTapUp: (_) => setState(() => _touched = null),
          onTapCancel: () => setState(() => _touched = null),
          onVerticalDragStart: (d) => touch(d.localPosition.dy),
          onVerticalDragUpdate: (d) => touch(d.localPosition.dy),
          onVerticalDragEnd: (_) => setState(() => _touched = null),
          child: Column(
            children: [
              for (final letter in _letters)
                SizedBox(
                  height: cell,
                  child: FittedBox(
                    child: Text(
                      letter,
                      style: TextStyle(
                        color: widget.existing.contains(letter)
                            ? color
                            : color.withValues(alpha: 0.35),
                        fontWeight: letter == _touched
                            ? FontWeight.w900
                            : FontWeight.w700,
                      ),
                    ),
                  ),
                ),
            ],
          ),
        );
      },
    );
  }
}
