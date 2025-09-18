# ADR-001: Selection of Plugin Architecture for Visual Identifier

* **Status**: Accepted
* **Date**: 2025-08-21
* **Authors**: [Your Name]

## 1. Context

We need to implement a JetBrains plugin that automatically adds a visual identifier to each IDE instance to help users distinguish between multiple open projects. This identifier must be a low-opacity text overlay on the editor's background, visible during task switching (e.g., using the Windows key or `Alt+Tab`).

The primary challenge is to find a stable and non-intrusive way to modify the IDE's UI on a per-project basis to display this text.

---

## 2. Decision Drivers

* **User Experience (UX)**: The identifier must be a subtle background element that doesn't obstruct the code editor or other UI components.
* **Feasibility**: The implementation must be achievable using the public or reasonably accessible parts of the IntelliJ Platform SDK.
* **Performance**: The solution should not introduce noticeable lag or consume excessive resources.
* **Per-Project Scope**: The identifier must be unique to each project and not affect other open IDE instances.

---

## 3. Considered Options

* **Strategy 1: Dynamic Image Generation**
    * **Description**: Programmatically generate a transparent PNG image with the project identifier text, save it to a temporary location, and set this image as the editor background using an internal API.
    * **Pros**:
        * Directly integrates with the IDE's built-in background image feature, ensuring the identifier is a true background element.
        * Fulfills the core UX requirement of being a subtle watermark that is visible during task switching.
        * The entire process can be done manually today by a user, confirming the validity of the desired final state. The plugin’s value is in automating this multi-step, error-prone workflow.
    * **Cons**:
        * Relies on a private, undocumented IDE property (`ide.background.image`), which is susceptible to breaking changes in future IDE updates.
        * Requires file I/O operations (image creation and deletion), which can be a minor performance concern and requires proper file cleanup logic.

* **Strategy 2: Direct Rendering**
    * **Description**: Use an `EditorCustomization` or `CustomHighlightingRenderer` to directly draw the text onto the editor pane's canvas.
    * **Pros**:
        * Avoids file I/O entirely, making it more performant and cleaner.
        * Relies on public APIs, reducing the risk of being broken by IDE updates.
    * **Cons**:
        * High implementation complexity, as it requires deep knowledge of the IDE's custom painting and rendering APIs.
        * The text may be rendered on top of code or other UI elements, potentially interfering with the user's coding experience.

* **Strategy 3: Alternative UI Component**
    * **Description**: Place the project identifier text in an existing, non-intrusive UI component like the status bar or a small tool window.
    * **Pros**:
        * Simple to implement using stable, well-documented APIs.
        * Extremely low risk of breaking with IDE updates.
    * **Cons**:
        * Fails the primary UX requirement. The identifier would not be a background watermark and would not be visible during a task switcher view, making it ineffective for its intended purpose.

---

## 4. Decision

We have decided to adopt **Strategy 1: Dynamic Image Generation**. This approach is the only one that directly addresses the core user experience requirement of displaying a subtle background identifier. While it relies on an internal API, the direct benefit of a "true" background watermark outweighs the risk of future maintenance. The alternative methods either fail to meet the core UX goal or introduce unnecessary implementation complexity.

---

## 5. Consequences

* **Positive**:
    * The plugin will deliver a user experience that exactly matches the original vision.
    * The implementation is relatively straightforward and can be developed quickly.
* **Negative**:
    * The plugin's core functionality is reliant on an internal IDE property, meaning it may require maintenance with each major IDE version release.
    * File management is required to handle image creation and cleanup to prevent temporary file accumulation.

---

## 6. Related Documents

* [Initial Plugin Idea and Research](link to research document)



---

### Operational note: Settings location

For users, the plugin's settings can be accessed via:

File | Settings | Appearance & Behavior | Appearance
