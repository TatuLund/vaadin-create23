# Default Address and Supervisor in Storefront

## 1. Modify purchase wizard

Storefront package in vaadincreate-ui module has PurchaseWizard component. The steps 2 and 3 needs to be updated so that they will use default Address and Supervisor (type User) if the Requester has those.

### 1.1. Default Address

When entering Step 2. in Purchase Wizard prefil the Address form with default address.

The default Address to be used is determined based on if the Requester has previous purchases. Find the last purchase and use its Address as default. If there are no previous purchases, no default Address will be applied

### 1.2. Default Supervisor

When entering Step 3. in Purchase Wizard set the default Supervisor selected in supervisor ComboBox. 

The default Supervisor to be used is determined based on if the Requester has previous purchases. Find the last purchase and use its approver as Supervisor. If there is no previous purchases, no default Supervisor will be selected.

### 1.3. StorefrontPresenter

- Add method in StorefrontPresenter to fetch the optional default Address

- Add method in StorefrontPresenter to fetch the optional default Supervisor (type User)

## 2. Acceptance criteria

### 2.1. Tests

The requested change will change behavior. Thus tests needs to be updated

- StorefrontViewTest: Update the tests to use "Customer8" / "customer8" as it does not have default Address or Supervisor, and does not conflict with other tests.
- This will require splitting two old tests to new test class StorefrontViewDefaultTest: should_ShowStorefrontView_When_CustomerAccesses and clicking_grid_row_toggles_details_visibility which will need to be run with user "Customer11" / "customer11" as the tests are required existing purchase data.
- Also move  public void purchase_history_grid_refreshes_when_purchase_updated_event_is_fired and purchase_history_grid_refreshes_when_wizard_is_submitted to StorefrontViewDefaultTest as they have side effects.
- Add a new test to StorefrontViewDefaultTest which validates that default address is being picked from the previous purchase and default supervisor is selected.

### 2.2. Other

- The PurchaseWizard shall use Presenter to access the PurchaseService. 
- No UI changes. This feature does not require new components to be added in PurchaseWizard.
