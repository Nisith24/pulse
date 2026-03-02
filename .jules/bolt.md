
## 2024-05-28 - Compose Performance: Lazy List Anti-patterns
**Learning:** Performing heavy calculations (like Regex compilation or complex list filtering/mapping) directly inside a Lazy list's content block or `items` definition causes extreme performance degradation, as these run on every composition and potentially on every scroll calculation.
**Action:** Always extract heavy operations like Regex initialization, list filtering (`.filter`), and mapping out of the `LazyColumn`/`LazyVerticalGrid` and wrap them in a `remember` block tied to their underlying state dependencies. This guarantees O(1) evaluation on scroll and limits operations to actual data changes.
