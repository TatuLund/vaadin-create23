# Ubiquitous Language

This appendix defines the preferred business vocabulary for VaadinCreate.

Use the canonical term in new requirements, documentation, discussions, and code comments. Existing UI labels, test names, class names, or legacy text may still use older wording when compatibility matters.

## Core terms

| Canonical term | Related terms / legacy wording | Definition |
| --- | --- | --- |
| Product | Book, catalog item | A sellable item managed in inventory and available for purchase workflows. `Product` is the preferred domain term; `book` is legacy sample wording. |
| Category | Product category | A grouping used to organize products and drive statistics. |
| Availability | AVAILABLE, COMING, DISCONTINUED | The sellability state of a product. Availability is related to, but distinct from, stock count. |
| Stock count | Inventory quantity, stock | The number of units currently available in inventory for a product. |
| Current user | Signed-in user, session user | The authenticated user stored in session and used for role-based behavior. |
| Requester | Customer, employee | The user who creates a purchase request in the Storefront flow. `Requester` is the preferred neutral term; demo roles may still be described as customer or employee in UI text. |
| Supervisor | Approver selection | The person chosen by the requester in the wizard to review the request. In the approval flow this same person acts as the approver. |
| Approver | Supervisor in approval context | The user responsible for approving or rejecting pending purchase requests. |
| Purchase request | Pending purchase, submitted request | A request created by a requester and sent for approval. This is the preferred term before an approval decision is made. |
| Purchase | Purchase record, request record | The persisted aggregate tracked in history, approvals, exports, and statistics. A purchase may still be referred to as a purchase request when its status is pending. |
| Purchase line | Line item | A single product entry within a purchase, including quantity and unit price. |
| Cart | Order summary, selected items | The in-memory set of currently selected products and quantities in the storefront wizard before submission. |
| Delivery address | Shipping address | The address collected during the storefront flow for fulfilling a purchase. |
| Draft | Saved draft, draft item set | A persisted work-in-progress representation related to a user's unfinished product selection. It is distinct from the in-memory cart. |
| Purchase history | History grid, purchases history | The list of persisted purchases visible to requesters and approvers/admins. |
| Approval | Decision, approve/reject action | The business decision taken by an approver on a pending purchase request. |
| Purchase status | `PurchaseStatus`, state | The lifecycle state of a purchase, such as pending, completed, rejected, or cancelled. |
| System message | Broadcast message, admin message | A timestamped message distributed through the system and surfaced to users through the UI. |
| Admin note | About note | The editable rich-text content shown in the About view. It may display the latest system message, but it is a UI surface, not the domain event itself. |
| Object lock | Lock, locked object | A coordination record showing that a domain object is currently being edited by a specific user. |

## Naming guidance

- Prefer `product` over `book` unless referring to legacy UI labels, tests, or compatibility constraints.
- Prefer `requester` over `customer` or `employee` when the behavior is the same regardless of demo role.
- Use `supervisor` in the storefront step where the requester selects a reviewer.
- Use `approver` in the approvals flow where that reviewer makes a decision.
- Use `purchase request` when emphasizing submission and approval.
- Use `purchase` when emphasizing the persisted record, history, export, or statistics.
