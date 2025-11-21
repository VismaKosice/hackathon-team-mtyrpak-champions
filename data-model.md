# Data Model

## Overview

The pension calculation engine operates on a **Situation** object that represents the complete state of a pension dossier at a specific point in time. Mutations modify this situation sequentially to produce the final calculated state.

## Entity Relationship Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                        Situation                            │
│  (Complete calculation state at a point in time)            │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        │ contains (1:1)
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│                         Dossier                             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ dossier_id: string (UUID)                            │   │
│  │ status: "ACTIVE" | "RETIRED"                         │   │
│  │ retirement_date: date (optional)                     │   │
│  └──────────────────────────────────────────────────────┘   │
└───────┬───────────────────────────────────────┬─────────────┘
        │                                       │
        │ contains (1:N)                        │ contains (1:N)
        │                                       │
        ▼                                       ▼
┌──────────────────────┐          ┌──────────────────────────┐
│       Person         │          │        Policy            │
│                      │          │                          │
│ person_id: UUID      │          │ policy_id: string        │
│ role:                │          │ scheme_id: string        │
│   "PARTICIPANT" |    │          │ employment_start_date    │
│   "PARTNER"          │          │ salary: number           │
│ name: string         │          │ part_time_factor: 0-1    │
│ birth_date: date     │          │ attainable_pension:      │
│ relationship_start_  │          │   number (calculated)    │
│   date: date (opt)   │          │                          │
│ relationship_end_    │          │                          │
│   date: date (opt)   │          │                          │
└──────────────────────┘          └──────────────────────────┘
```

## Entity Descriptions

### Situation

The root object containing the complete calculation state.

**Properties:**
- `dossier` (required): The dossier object

**Example:**
```json
{
  "dossier": {
    "dossier_id": "550e8400-e29b-41d4-a716-446655440000",
    "status": "ACTIVE",
    "retirement_date": null,
    "persons": [...],
    "policies": [...]
  }
}
```

---

### Dossier

Represents a pension participant's complete record.

**Properties:**
- `dossier_id` (required, string, UUID): Unique identifier for the dossier
- `status` (required, enum): Current status - `"ACTIVE"` or `"RETIRED"`
- `retirement_date` (optional, date): Date of retirement (set by `calculate_retirement_benefit`)
- `persons` (required, array): List of persons associated with the dossier
- `policies` (required, array): List of pension policies

**Status Transitions:**
- Initially: `"ACTIVE"` (set by `create_dossier`)
- After `calculate_retirement_benefit`: `"RETIRED"`

---

### Person

Represents an individual associated with a dossier.

**Properties:**
- `person_id` (required, UUID): Unique identifier for the person
- `role` (required, enum): Person's role - `"PARTICIPANT"` or `"PARTNER"`
- `name` (required, string): Full name
- `birth_date` (required, date): Date of birth
- `relationship_start_date` (optional, date): When relationship started (for PARTNER)
- `relationship_end_date` (optional, date): When relationship ended (for PARTNER)

**Roles:**
- `"PARTICIPANT"`: The primary pension participant (created by `create_dossier`)
- `"PARTNER"`: Spouse or partner (added by `add_relationship` - not in scope for Visma Performance Hackathon)

**Notes:**
- Each dossier must have exactly one `PARTICIPANT`
- Multiple `PARTNER` entries are possible (not in scope for Visma Performance Hackathon)

---

### Policy

Represents a pension scheme/policy within a dossier.

**Properties:**
- `policy_id` (required, string): Unique identifier (format: `{dossier_id}-{policy_number}`)
- `scheme_id` (required, string): Identifier of the pension scheme
- `employment_start_date` (required, date): Start date of employment for this policy
- `salary` (required, number): Annual full-time salary (updated by `change_salary`)
- `part_time_factor` (required, number, 0-1): Part-time employment factor (1.0 = full-time)
- `attainable_pension` (optional, number): Calculated annual pension benefit (set by `calculate_retirement_benefit`)

**Policy ID Generation:**
- Format: `{dossier_id}-{sequence_number}`
- First policy: `{dossier_id}-1`
- Second policy: `{dossier_id}-2`
- Sequence number is based on mutation order (not mutation_sequence_number)

**Notes:**
- Salary is the full-time equivalent salary
- Effective salary = `salary * part_time_factor`
- `attainable_pension` is only set after `calculate_retirement_benefit` mutation

---

## Data Flow

### Initial State

Before any mutations:
```json
{
  "dossier": null
}
```

### After `create_dossier`

```json
{
  "dossier": {
    "dossier_id": "550e8400-e29b-41d4-a716-446655440000",
    "status": "ACTIVE",
    "retirement_date": null,
    "persons": [
      {
        "person_id": "660e8400-e29b-41d4-a716-446655440001",
        "role": "PARTICIPANT",
        "name": "John Doe",
        "birth_date": "1980-01-01"
      }
    ],
    "policies": []
  }
}
```

### After `add_policy`

```json
{
  "dossier": {
    "dossier_id": "550e8400-e29b-41d4-a716-446655440000",
    "status": "ACTIVE",
    "retirement_date": null,
    "persons": [...],
    "policies": [
      {
        "policy_id": "550e8400-e29b-41d4-a716-446655440000-1",
        "scheme_id": "SCHEME_001",
        "employment_start_date": "2000-01-01",
        "salary": 50000,
        "part_time_factor": 1.0
      }
    ]
  }
}
```

### After `change_salary`

```json
{
  "dossier": {
    "dossier_id": "550e8400-e29b-41d4-a716-446655440000",
    "status": "ACTIVE",
    "retirement_date": null,
    "persons": [...],
    "policies": [
      {
        "policy_id": "550e8400-e29b-41d4-a716-446655440000-1",
        "scheme_id": "SCHEME_001",
        "employment_start_date": "2000-01-01",
        "salary": 60000,  // Updated
        "part_time_factor": 1.0
      }
    ]
  }
}
```

### After `calculate_retirement_benefit`

```json
{
  "dossier": {
    "dossier_id": "550e8400-e29b-41d4-a716-446655440000",
    "status": "RETIRED",  // Updated
    "retirement_date": "2025-01-01",  // Set
    "persons": [...],
    "policies": [
      {
        "policy_id": "550e8400-e29b-41d4-a716-446655440000-1",
        "scheme_id": "SCHEME_001",
        "employment_start_date": "2000-01-01",
        "salary": 60000,
        "part_time_factor": 1.0,
        "attainable_pension": 30000  // Calculated and set
      }
    ]
  }
}
```

---

## Key Relationships

1. **Situation → Dossier:** One-to-one (situation always contains exactly one dossier or null)
2. **Dossier → Persons:** One-to-many (dossier has at least one person - the participant)
3. **Dossier → Policies:** One-to-many (dossier can have zero or more policies)

## Validation Rules

### Dossier
- Must have exactly one person with role `"PARTICIPANT"`
- `status` must be `"ACTIVE"` or `"RETIRED"`
- If `status` is `"RETIRED"`, `retirement_date` must be set

### Person
- `person_id` must be unique within the dossier
- `birth_date` must be a valid date
- `role` must be `"PARTICIPANT"` or `"PARTNER"`

### Policy
- `policy_id` must be unique within the dossier
- `salary` must be >= 0
- `part_time_factor` must be between 0 and 1 (inclusive)
- `employment_start_date` must be a valid date
- If `attainable_pension` is set, it must be >= 0

---

## UUID Generation

All UUIDs (`dossier_id`, `person_id`) should be generated as standard UUID v4 values.

**Example:** `550e8400-e29b-41d4-a716-446655440000`

---

## Date Formats

All dates use ISO 8601 format: `YYYY-MM-DD`

**Examples:**
- `"2000-01-01"`
- `"2025-12-31"`

---

## Number Formats

- **Salary:** Decimal number (e.g., `50000.00` or `50000`)
- **Part-time factor:** Decimal between 0 and 1 (e.g., `0.8`, `1.0`)
- **Attainable pension:** Decimal number (e.g., `30000.00`)

