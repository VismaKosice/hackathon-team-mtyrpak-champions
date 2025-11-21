# Quick Start Guide

## Files Overview

- **`README.md`** - Complete requirements and specifications
- **`api-spec.yaml`** - OpenAPI 3.0.0 specification for the API endpoint
- **`data-model.md`** - Visual data model and entity relationships
- **`mutation-definitions/`** - Reference JSON schema examples for each mutation (for understanding the structure)
  - `create_dossier.json`
  - `add_policy.json`
  - `change_salary.json`
  - `calculate_retirement_benefit.json`

## Getting Started Checklist

1. ✅ Read `README.md` for complete requirements
2. ✅ Review `api-spec.yaml` for API contract
3. ✅ Understand data model in `data-model.md`
4. ✅ Review mutation definition examples in `mutation-definitions/` (reference for structure)
5. ⏳ Wait for sample requests/responses (TODO)
6. ⏳ Set up development environment
7. ⏳ Implement calculation engine
8. ⏳ Optimize for performance
9. ⏳ Test with provided testing framework
10. ⏳ Submit solution

## Key Points to Remember

### Dynamic Mutation Support
- **CRITICAL:** Mutations must be isolated in a separate assembly/module from the main engine
- Mutations can be deployed independently without redeploying the main calculation engine
- Each mutation assembly contains both:
  - **Mutation definitions** (schema/metadata)
  - **Business logic** with two steps:
    1. **Business validation** (check prerequisites)
    2. **Calculation logic** (apply mutation to state)
- Main engine must be generic - no hardcoded mutation types
- New mutations added by implementing in mutation assembly and deploying only that assembly

### Mutations Must Be Processed Sequentially
- Mutations are applied in the order provided
- Each mutation modifies the calculation state
- Order matters for correct calculations

### Policy ID Generation
- `policy_id` is auto-generated: `{dossier_id}-{sequence_number}`
- First policy: `{dossier_id}-1`
- Second policy: `{dossier_id}-2`
- Sequence based on order in mutations array

### Complex Mutation: calculate_retirement_benefit
- Validates eligibility (age ≥65 OR years of service ≥40)
- Calculates weighted average salary
- Applies pension formula: `average_salary * years_of_service * 0.02`
- Updates dossier status to RETIRED
- Sets `attainable_pension` for each policy

### Performance Focus
- Optimize calculation time
- Support parallel request processing
- Maximize throughput

### Docker Requirement
- Must be deployable via Docker
- Application on port 8080 (configurable)
- All dependencies containerized

## Common Pitfalls to Avoid

1. **Not isolating mutations** - Mutations must be in a separate assembly/module from the main engine
2. **Hardcoding mutation logic in engine** - Main engine must be generic, mutation-specific logic belongs in mutation assembly
2. **Not processing mutations sequentially** - Order matters!
3. **Incorrect policy_id generation** - Must follow `{dossier_id}-{sequence_number}` format
4. **Missing validation** - Validate all business rules
5. **Ignoring part-time factor** - Use `salary * part_time_factor` for effective salary
6. **Incorrect retirement calculation** - Follow the formula exactly
7. **Not handling errors properly** - Return appropriate HTTP status codes and messages

## Questions?

Refer to:
- `README.md` for detailed requirements
- `api-spec.yaml` for API contract details
- `data-model.md` for data structure clarifications

Good luck! 🚀

