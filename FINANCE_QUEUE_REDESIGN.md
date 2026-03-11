# Finance Queue Request Redesign

## Overview
Redesign the Finance queue request to be a multi-step form similar to Registrar request.

## Steps

### Step 1: Request Queue
- **Title**: "Request Queue"
- **Subtitle**: "Please select the document details below. This will only take a few minutes."
- **Fields**:
  - Transaction Type (Radio buttons):
    - Request Registrar Queue
    - Request Finance Queue (selected by default)
  - Student Details section:
    - Name (text input with placeholder)
    - Course/Strand (text input with placeholder)
    - Student ID (text input with placeholder)
    - Mobile (text input with placeholder)
- **Button**: "Next →"

### Step 2: Payment Slip
- **Title**: "Payment Slip"
- **Subtitle**: "Please fill in your personal details below. This will only take a few minutes."
- **Progress**: Step 1 of 2
- **Fields**:
  - Personal data section:
    - Name (auto-filled, read-only)
    - Course/Strand (auto-filled, read-only)
    - Date (date picker)
    - Student ID (auto-filled, read-only)
    - Mobile (auto-filled, read-only)
    - Purpose of payment (dropdown):
      - Paying for tuition
      - Paying for fees
      - Other payments
- **Button**: "Next →"

### Step 3: Payment Method
- **Title**: "Payment Method"
- **Subtitle**: "Please select the document details below. This will only take a few minutes."
- **Progress**: Step 2 of 2
- **Fields**:
  - Amount Paid section:
    - Cash Denomination (text with example)
    - No. of Pieces (text with example)
    - Amount (text with example)
  - For Card Payment section:
    - Payment Method (dropdown): Gcash, Cash, Card, Cheque
    - **Dynamic fields based on selection**:
      
      **If Gcash selected**:
      - Gcash Name
      - Amount
      
      **If Cash selected**:
      - Gcash Name (reused field)
      - Amount
      
      **If Card selected**:
      - Card Holder Name
      - Bank
      - Amount
      
      **If Cheque selected**:
      - Cheque No.
      - Bank
      - Amount
- **Button**: "Submit"

## Implementation Plan
1. Create 3 new layout files for each step
2. Update BookAppointmentFragment to handle multi-step navigation
3. Store data between steps
4. Submit all data at the end

## Notes
- Auto-fill student data from Firestore
- Validate all fields before proceeding
- Show progress indicator
- Allow back navigation between steps
