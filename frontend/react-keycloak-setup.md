## Product Catalog & Management

This section covers how products are displayed to users and managed by vendors and administrators.

### Managing My Product Listings (Vendor Portal)
This section allows authenticated vendors to manage their own product listings. This includes adding new products (with custom attributes based on selected Product Types), editing existing ones, and deleting them.

#### 1. Routing and Navigation (Vendor Portal)
Ensure routes for vendor product management are set up in your main router (`App.js` or `App.tsx`) and protected by `VendorRoute` (which checks for the "vendor" role).

*   `/vendor/products`: Renders `VendorProductListPage` to list the vendor's own products.
*   `/vendor/products/new`: Renders `ProductFormPage` for creating a new product.
*   `/vendor/products/edit/:productId`: Renders `ProductFormPage` for editing an existing product, identified by `productId`.

**Navigation Links:** Add a link to "My Products" (pointing to `/vendor/products`) in the vendor-specific navigation menu.

#### 2. Vendor Product List Page (`VendorProductListPage.js`/`.tsx`)
This component is responsible for fetching and displaying the products that belong to the currently authenticated vendor. 
*   It fetches data from a vendor-specific endpoint, typically `GET /api/v1/products/vendor/{vendorId}`. The `{vendorId}` is the Keycloak User ID of the logged-in vendor, which can be obtained from the `keycloak.tokenParsed.sub` claim.
*   The page displays a list of the vendor's products, showing relevant details (e.g., name, price, type).
*   It includes an "Add New Product" button that navigates to `/vendor/products/new`. This route renders the `ProductFormPage.js` component, allowing the vendor to create a new product.
*   Each product in the list has an "Edit" button, which navigates to `/vendor/products/edit/:productId`. This route also renders the `ProductFormPage.js` component, but pre-filled with the data of the specified product for modification.
*   A "Delete" button is provided for each product, which, upon confirmation, calls the `DELETE /api/v1/products/:productId` endpoint. Backend security ensures that only the product owner (vendor) can delete their own product.

**(The full code for `VendorProductListPage.js`/`.tsx` would include state for products, loading, and errors, `useEffect` hooks for fetching data based on `keycloak.tokenParsed.sub`, and functions for handling product deletion. The JSX would map over the products to display them and include `Link` components for navigation and buttons for actions.)**

#### 3. Using the Product Form (`ProductFormPage.js`) for Vendor Listings

When a vendor adds a new product or edits an existing one from their "My Product Listings" page, they will use the common `ProductFormPage` component. This form is designed to be dynamic and adapts to the selected `ProductType`.

**Key points for vendors using this form:**

*   **Automatic `vendorId`:** The `vendorId` for products created by a vendor is automatically associated with their account by the backend, based on their authentication token. Vendors do not need to input this information. The `ProductFormPage` (detailed in a separate guide) includes logic to hide or omit the `vendorId` input if the user is not an admin.
*   **Product Type Selection:** Vendors will select from the list of administrator-defined `ProductTypes`.
*   **Dynamic Custom Fields:** Based on the selected `ProductType`, the form will display the appropriate custom fields for that type, alongside common product fields like name, description, and price.

For a comprehensive guide on the detailed implementation of `ProductFormPage.js`, including its state management for common and custom fields, dynamic rendering logic, edit mode initialization, and form submission procedures, please see:

➡️ **[Detailed Guide: Reusable Product Form Component (`ProductFormPage.js`)]`./product-form-page-guide.md`)**

### Managing All Product Listings (Admin Panel)
This section allows administrators to manage all product listings across the platform. It involves listing all products from all vendors and providing administrative actions like editing or deleting any product.

#### 1. Routing and Navigation (Admin)
Ensure routes for admin product management are set up in your main router (`App.js` or `App.tsx`) and protected by `AdminRoute`.

*   `/admin/products`: Renders `AdminProductListPage` to list all products.
*   Admins will use the same shared form routes for creating/editing products:
    *   `/vendor/products/new` (for creating a new product, potentially on behalf of a vendor).
    *   `/vendor/products/edit/:productId` (for editing any existing product).

**Navigation Links:** Add a link to "All Products" (pointing to `/admin/products`) in the admin navigation menu.

#### 2. Admin Product List Page (`AdminProductListPage.js`/`.tsx`)
This component is responsible for:
*   Fetching all products from the `GET /api/v1/products` endpoint.
*   Displaying products in a list or table, showing key details like name, vendor ID (if applicable), price, and product type.
*   Providing "Edit" and "Delete" buttons for each product.
    *   The "Edit" button navigates to `/vendor/products/edit/:productId`.
    *   The "Delete" button typically calls `DELETE /api/v1/products/:productId`. Admins usually have blanket permissions to delete any product, but this is enforced by the backend.
*   An "Add New Product" button that navigates to `/vendor/products/new`, allowing an admin to use the shared `ProductFormPage`.

**(Full component code for `AdminProductListPage.js`/`.tsx` would be similar to `VendorProductListPage` but fetches all products and doesn't filter by vendor ID on the client-side. It might also display more administrative information per product.)**

#### Using the Product Form (`ProductFormPage.js`) by Administrators

Administrators use a common, dynamic form (`ProductFormPage.js`) for both creating new products and editing existing ones. This form adapts to the selected `ProductType` to show relevant common and custom fields.

*   For detailed information on the core implementation, state management, dynamic field rendering, edit mode, and submission logic of this reusable form, please refer to:
    ➡️ **[Detailed Guide: Reusable Product Form Component (`ProductFormPage.js`)]`./product-form-page-guide.md`)**

*   For specific details on how administrators can assign a `vendorId` when using this form, and other admin-specific considerations, please see:
    ➡️ **[Admin-Specific Adaptations for Product Form]`./admin-product-form-adaptations.md`)**
