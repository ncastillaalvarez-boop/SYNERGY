class JsonUtils {
  JsonUtils._();

  static String? string(dynamic value) => value == null ? null : value.toString();

  static bool boolValue(dynamic value, {bool fallback = false}) {
    if (value is bool) return value;
    return value.toString().toLowerCase() == 'true' ? true : fallback;
  }

  static DateTime? dateTime(dynamic value) {
    if (value == null || value.toString().isEmpty) return null;
    return DateTime.tryParse(value.toString());
  }

  static List<String> strings(dynamic value) {
    if (value is! Iterable) return const [];
    return value.whereType<dynamic>().map((item) => item.toString()).toList();
  }

  static String? dateOnly(DateTime? value) {
    if (value == null) return null;
    final month = value.month.toString().padLeft(2, '0');
    final day = value.day.toString().padLeft(2, '0');
    return '${value.year}-$month-$day';
  }

  static Map<String, dynamic> withoutNulls(Map<String, dynamic> value) {
    value.removeWhere((_, item) => item == null);
    return value;
  }
}
