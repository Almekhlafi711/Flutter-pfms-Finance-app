class Person {
  final String id;
  final String name;
  final String? phone;
  final String category;
  final String currency;
  final String notes;
  final bool isActive;
  final int createdAt;

  Person({
    required this.id,
    required this.name,
    this.phone,
    this.category = "General",
    this.currency = "SAR",
    this.notes = "",
    this.isActive = true,
    int? createdAt,
  }) : createdAt = createdAt ?? DateTime.now().millisecondsSinceEpoch;

  Person copyWith({
    String? id,
    String? name,
    String? phone,
    String? category,
    String? currency,
    String? notes,
    bool? isActive,
    int? createdAt,
  }) {
    return Person(
      id: id ?? this.id,
      name: name ?? this.name,
      phone: phone ?? this.phone,
      category: category ?? this.category,
      currency: currency ?? this.currency,
      notes: notes ?? this.notes,
      isActive: isActive ?? this.isActive,
      createdAt: createdAt ?? this.createdAt,
    );
  }
}
