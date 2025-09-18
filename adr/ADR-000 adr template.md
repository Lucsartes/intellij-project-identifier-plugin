# [Title]

* **Status**: [Proposed | Accepted | Rejected | Superseded by [ADR-00X](link to superseding ADR)]
* **Date**: [YYYY-MM-DD]
* **Authors**: [Author Name(s)]

## 1. Context

[Describe the technical dilemma or the problem you are facing. What is the business or technical driver for this decision? Frame the problem neutrally and objectively. What are the forces at play? What is the current state?
Example: "We need to choose a new database solution for our user profile service. The current MySQL database is no longer meeting our performance requirements for read-heavy workloads, and our data model is becoming more document-centric."]

## 2. Decision Drivers

[List the key factors that influenced this decision. These should be the high-level goals and constraints.
Examples:
* Scalability: The solution must handle a 10x increase in read traffic.
* Cost: The total cost of ownership (TCO) must be within our budget.
* Operational Overhead: The solution should be easy to manage and monitor with our current tools.
* Developer Familiarity: The team should have prior experience with the technology.]

## 3. Considered Options

[For each option, provide a brief description and a critical analysis against the decision drivers. This section should be objective and present a balanced view of the pros and cons of each alternative.
* **[Option A]**
    * Description: [Brief summary of the technology or approach.]
    * Pros:
        * [Benefit 1]
        * [Benefit 2]
    * Cons:
        * [Drawback 1]
        * [Drawback 2]
* **[Option B]**
    * ...
* **[Option C]**
    * ...
]

## 4. Decision

[State the final decision clearly and explicitly. Justify why the chosen option was selected over the others, referencing the decision drivers.
Example: "We have chosen to adopt MongoDB as the primary database for the user profile service. This decision was driven primarily by its strong horizontal scalability for read operations and its native support for a flexible document data model, which aligns with our future needs. While it introduces some operational complexity, the benefits in performance and developer agility outweigh this concern."]

## 5. Consequences

[Detail the positive, negative, and neutral impacts of the decision. What will need to change as a result? What are the new challenges or opportunities?
* **Positive**:
    * [Benefit 1]
    * [Benefit 2]
* **Negative**:
    * [Drawback 1]
    * [Drawback 2]
* **Neutral**:
    * [Changes that are neither good nor bad, but simply need to be done.]

]

## 6. Related Documents

[Link to any relevant documentation, design proposals, or other ADRs that are related to this decision.]


---
### File Naming Convention

The recommended format for the ADR file name is:

**`ADR-NNN-short-title-in-kebab-case.md`**

* **`NNN`**: A sequential, zero-padded number (e.g., `001`, `002`, `003`). This makes it easy to sort and reference the ADRs in chronological order.
* **`short-title-in-kebab-case`**: A brief, descriptive title that is a lowercase, hyphen-separated version of the ADR's title.

