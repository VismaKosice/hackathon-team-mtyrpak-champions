# Visma Performance Hackathon: Pension Calculation Engine

## Event Overview

**Event Name:** Visma Performance Hackathon  
**Duration:** 1 day (8:00 AM - 6:00 PM)  
**Team Size:** 2-3 people  
**Focus:** Build a high-performance pension calculation engine that processes sequential mutations to calculate pension entitlements.

---

## Objective

Design and implement a pension calculation engine that processes ordered sequences of mutations (operations) to calculate pension entitlements for participants. The engine must be **optimized for performance** while maintaining correctness and clean architecture.

---

## Core Requirements

### 1. API Endpoint

Implement a **single HTTP endpoint** that processes calculation requests:

- **Endpoint:** `POST /calculation-requests`
- **Content-Type:** `application/json`
- **Response:** `application/json`

The complete API specification is provided in `api-spec.yaml` (OpenAPI 3.0.0 format).

### 2. Mutation Processing

The engine must process mutations **sequentially** in the order provided. Each mutation modifies the calculation state (situation), and the final state represents the calculated pension entitlements.

**Required Mutations:**

1. **`create_dossier`** - Creates a new pension participant dossier
2. **`add_policy`** - Adds a pension policy to an existing dossier (policy_id is auto-generated as `{dossier_id}-{sequence_number}`)
3. **`change_salary`** - Updates salary for a specific policy
4. **`calculate_retirement_benefit`** - Complex mutation that calculates retirement benefits (see details below)

**Note:** The `policy_id` for policies is generated automatically by the system. The format is `{dossier_id}-{sequence_number}`, where sequence_number starts at 1 for the first policy and increments for each subsequent policy added to the dossier.

### 3. Dynamic Mutation Support

The application **MUST** support adding new mutations without requiring redeployment of the main calculation engine.

**Requirements:**
- Mutations must be defined as part of the application code, but **isolated from the main engine logic** (e.g., in a separate assembly, module, or package)
- The mutation assembly/module must be deployable independently from the main calculation engine
- The mutation assembly/module contains both:
  1. **Mutation definitions**: Schema/metadata information about each mutation (property structure, validation rules, etc.)
  2. **Business logic**: Implementation of each mutation consisting of two steps:
     - **Business validation**: Check if prerequisites are met for the mutation (e.g., for `change_salary`, verify that the policy exists)
     - **Calculation logic**: Apply the mutation's calculation logic to modify the calculation state
- The main calculation engine must be implemented generically to discover and load mutations from the separate assembly/module
- New mutations can be added by creating a new mutation implementation in the mutation assembly and deploying only that assembly (without redeploying the main engine)
- The main engine must not contain hardcoded mutation types or mutation-specific logic

**Implementation Considerations:**
- The mutation assembly/module should be discoverable by the main engine (e.g., via dependency injection, plugin system, or module loading)
- Mutation definitions and business logic should be co-located in the same assembly/module for each mutation
- The main engine should provide a generic interface/contract that mutation implementations must follow
- Consider caching mutation instances for performance
- The application should handle invalid or missing mutation implementations gracefully
- Teams are free to choose the architecture pattern (e.g., separate DLL/JAR/npm package, plugin system, etc.) that best fits their technology stack

**Note:** The `mutation-definitions/` folder contains reference JSON schema examples that illustrate the structure and properties of each mutation. These are provided for reference only - mutations should be implemented as code in the mutation assembly/module, not loaded from JSON files.

### 4. Data Model

The calculation state (situation) contains:
- **Dossier**: Participant information, status, retirement date
- **Persons**: List of persons (participant, partner)
- **Policies**: List of pension policies with salary, part-time factor, and calculated benefits

See `data-model.md` for a visual representation of the data model and relationships.

### 5. Complex Mutation: `calculate_retirement_benefit`

This mutation requires sophisticated business logic:

**Purpose:** Calculate retirement benefits for a participant based on their employment history.

**Properties:**
```json
{
  "dossier_id": "string",
  "retirement_date": "date"
}
```

**Business Rules:**

1. **Eligibility Validation:**
   - Participant must be at least **65 years old** on the retirement date, OR
   - Participant must have accumulated **40 or more years of service** across all policies

2. **Years of Service Calculation:**
   - Calculate total years of service from all active policies
   - For each policy: years = (retirement_date - employment_start_date) in years
   - Sum all years across policies (accounting for overlapping periods if needed)

3. **Average Salary Calculation:**
   - Calculate weighted average salary across all policies
   - Weight each policy's salary by its years of service
   - Formula: `weighted_avg = Σ(salary_i * years_i) / Σ(years_i)`
   - Adjust for part-time factor: `effective_salary = salary * part_time_factor`

4. **Pension Benefit Calculation:**
   - Apply pension formula: `annual_pension = average_salary * years_of_service * 0.02`
   - The factor `0.02` represents 2% per year of service
   - Distribute the annual pension across policies proportionally based on years of service

5. **State Updates:**
   - Set dossier `status` to `"RETIRED"`
   - Set dossier `retirement_date` to the provided retirement date
   - Update each policy's `attainable_pension` field with the calculated portion

**Validation Errors:**
- If participant is under 65 AND has less than 40 years of service → CRITICAL error
- If dossier does not exist → CRITICAL error
- If no policies exist → CRITICAL error
- If retirement_date is before any policy's employment_start_date → WARNING

**Example Calculation:**
```
Policy 1: employment_start_date = 2000-01-01, salary = 50000, part_time_factor = 1.0
Policy 2: employment_start_date = 2010-01-01, salary = 60000, part_time_factor = 0.8
retirement_date = 2025-01-01

Years of service:
- Policy 1: 25 years
- Policy 2: 15 years
Total: 40 years

Average salary (weighted):
- Policy 1: 50000 * 1.0 * 25 = 1,250,000
- Policy 2: 60000 * 0.8 * 15 = 720,000
- Total weighted: 1,970,000
- Total years: 40
- Average: 49,250

Annual pension: 49,250 * 40 * 0.02 = 39,400

Distribution:
- Policy 1: 39,400 * (25/40) = 24,625
- Policy 2: 39,400 * (15/40) = 14,775
```

### 6. Performance Requirements

**Optimize for performance!** The testing framework will measure:

1. **Calculation Time:** Time to process a single calculation request
2. **Parallel Processing:** Ability to handle multiple concurrent requests
3. **Throughput:** Total number of requests processed in a given time period

Teams will be compared primarily on performance metrics, but code quality and architecture will also be evaluated.

### 7. Docker Deployment

The application must be deployable using Docker. All dependencies (databases, caches, etc.) must be included in the Dockerfile or docker-compose setup.

**Requirements:**
- Provide a `Dockerfile` in the repository root
- Application must start and be accessible on port `8080` (configurable via environment variable)
- All required services (databases, message queues, etc.) must be containerized

**Example Dockerfile structure:**
```dockerfile
FROM [your-base-image]
WORKDIR /app
COPY . .
RUN [build-commands]
EXPOSE 8080
CMD [start-command]
```

### 8. Response Format

The response must match the OpenAPI specification exactly. Key requirements:

- Include `calculation_metadata` with timing information
- Include `calculation_result` with:
  - `messages`: Any validation errors or warnings (English only)
  - `mutations`: List of processed mutations with **mandatory JSON Patch documents**
  - `end_situation`: Final calculation state
  - `initial_situation`: Starting state (always empty situation with dossier: null)

**JSON Patch Generation (Mandatory):**
- JSON Patch documents are **required** for all mutations
- Each mutation must include:
  - `forward_patch_to_situation_after_this_mutation`: Patch from previous situation to situation after this mutation
  - `backward_patch_to_previous_situation`: Patch from situation after this mutation back to previous situation
- JSON Patch format follows RFC 6902
- **Performance Note:** Efficient JSON Patch generation is a key performance optimization opportunity

### 9. Error Handling

- Return appropriate HTTP status codes (200, 400, 500)
- Include validation messages in the response for invalid requests
- Message levels: CRITICAL (fatal error, halts processing) or WARNING (allows processing to continue)

---

## Evaluation Criteria

Teams will be evaluated on:

1. **Performance (Primary Focus):**
   - Calculation time per request
   - Ability to process requests in parallel
   - Overall throughput

2. **Code Quality:**
   - Clean, readable code
   - Proper error handling
   - Code organization and structure

3. **Architecture:**
   - Design patterns and principles
   - Scalability considerations
   - Maintainability
   - Dynamic mutation support (ability to add new mutations without redeploying the main engine)

4. **Correctness:**
   - All business requirements met
   - Mutations applied correctly
   - Calculation results accurate

---

## Testing Framework

A testing framework will be provided that:

1. **Validates Business Requirements:**
   - Verifies mutations are applied correctly
   - Checks calculation results match expected outcomes
   - Validates response structure matches OpenAPI spec

2. **Measures Performance:**
   - Single request calculation time
   - Parallel request processing capability
   - Throughput under load

3. **Test Scenarios:**
   - Simple calculations (1-3 mutations)
   - Complex calculations (10+ mutations)
   - Concurrent request handling
   - Error scenarios

---

## Deliverables

Teams must provide:

1. **Source Code Repository:**
   - Complete application code
   - Dockerfile for deployment
   - README with setup instructions (if needed)

2. **Deployment Ready:**
   - Application must be runnable via Docker
   - Must accept requests on the specified endpoint
   - Must respond according to the OpenAPI specification

**Note:** No documentation, presentations, or demos are required. Focus on the code and performance.

---

## Technology Stack

**No restrictions!** Teams are free to choose:
- Programming language (Java, C#, Python, Node.js, Go, Rust, etc.)
- Frameworks and libraries
- Databases (if needed)
- Caching solutions
- Any other tools or technologies

**Only requirement:** Must be deployable via Docker.

---

## Sample Requests & Responses

**TODO:** Sample calculation requests with expected responses will be provided separately. These will include:
- Simple calculation examples (1-3 mutations)
- Complex calculation examples (10+ mutations)
- Expected response structures for validation

For now, refer to the OpenAPI specification (`api-spec.yaml`) for request/response structure examples.

---

## Timeline

- **8:00 AM:** Kickoff and requirements review
- **8:30 AM - 5:30 PM:** Development time
- **5:30 PM - 6:00 PM:** Final testing and submission
- **6:00 PM:** Code submission deadline

---

## Questions & Support

During the event, organizers will be available to answer questions about:
- API specification clarifications
- Business rule interpretations
- Testing framework usage

---

## Getting Started

1. Review the OpenAPI specification (`api-spec.yaml`)
2. Review the data model (`data-model.md`)
3. Set up your development environment
4. Start implementing the calculation engine
5. Focus on performance optimization
6. Test with the provided testing framework
7. Submit your solution

**Good luck and have fun building a high-performance calculation engine!**

