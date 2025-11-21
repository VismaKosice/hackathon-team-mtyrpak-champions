# Visma Performance Hackathon Materials Index

This folder contains all materials needed for the Pension Calculation Engine - Visma Performance Hackathon.

## 📋 Documentation Files

### Main Requirements
- **`README.md`** - Complete requirements document with all specifications, business rules, and evaluation criteria
- **`QUICK_START.md`** - Quick reference guide and getting started checklist

### Technical Specifications
- **`api-spec.yaml`** - OpenAPI 3.0.0 specification for the `/calculation-requests` endpoint
- **`data-model.md`** - Visual data model showing entity relationships and data structures

## 🔧 Mutation Definitions (Reference Examples)

The `mutation-definitions/` folder contains reference JSON schema examples for each mutation. These are provided as examples to understand the mutation structure. **Note:** Mutations should be implemented as code in a separate assembly/module (see README.md for details).

1. **`create_dossier.json`** - Creates a new pension participant dossier
2. **`add_policy.json`** - Adds a pension policy to an existing dossier
3. **`change_salary.json`** - Updates salary for a specific policy
4. **`calculate_retirement_benefit.json`** - Complex mutation that calculates retirement benefits

## 📝 File Structure

```
hackathon/
├── README.md                          # Main requirements document
├── QUICK_START.md                     # Quick reference guide
├── INDEX.md                           # This file
├── api-spec.yaml                      # OpenAPI API specification
├── data-model.md                      # Data model documentation
└── mutation-definitions/              # Mutation JSON schemas
    ├── create_dossier.json
    ├── add_policy.json
    ├── change_salary.json
    └── calculate_retirement_benefit.json
```

## 🚀 Getting Started

1. Start with **`README.md`** for complete requirements
2. Review **`api-spec.yaml`** for API contract
3. Understand the data model in **`data-model.md`**
4. Review mutation definition examples in **`mutation-definitions/`** (reference for structure)
5. Use **`QUICK_START.md`** as a quick reference during development

## ⚠️ Important Notes

- **Sample requests/responses** will be provided separately (TODO in README.md)
- **Testing framework** will be provided by organizers
- Focus on **performance optimization** - this is the primary evaluation criterion
- All code must be **Docker-deployable**

## 📞 Support

During the Visma Performance Hackathon, organizers will be available to answer questions about:
- API specification clarifications
- Business rule interpretations
- Testing framework usage

---

**Good luck at the Visma Performance Hackathon building your high-performance calculation engine!** 🎯

