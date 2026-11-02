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
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'address.dart';
import 'addresses_controller.dart';

/// The thumbnail of an address, or its initials on a gradient (as the Contacts app of Apple).
class AddressAvatar extends ConsumerWidget {
  const AddressAvatar({
    super.key,
    required this.address,
    required this.size,
    this.full = false,
  });

  final Address address;
  final double size;

  /// The image itself instead of the thumbnail (for a large avatar).
  final bool full;

  /// The two colors of the gradient: always the same for an address.
  static List<Color> gradientOf(Address address) {
    final hue = (address.id * 47) % 360;
    return [
      HSLColor.fromAHSL(1, hue.toDouble(), 0.45, 0.68).toColor(),
      HSLColor.fromAHSL(1, (hue + 25) % 360, 0.45, 0.48).toColor(),
    ];
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final thumbnail = ref
        .watch(addressImageProvider((address: address, full: false)))
        .value;
    // The thumbnail stays until the image itself is there (or when it can't be loaded).
    final file = full
        ? ref
                  .watch(addressImageProvider((address: address, full: true)))
                  .value ??
              thumbnail
        : thumbnail;
    return Container(
      width: size,
      height: size,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: gradientOf(address),
        ),
        image: file == null
            ? null
            : DecorationImage(
                image: FileImage(file),
                fit: BoxFit.cover,
                filterQuality: FilterQuality.high,
              ),
      ),
      child: file != null
          ? null
          : Text(
              address.initials,
              style: TextStyle(
                color: Colors.white,
                fontSize: size * 0.4,
                fontWeight: FontWeight.w500,
              ),
            ),
    );
  }
}
