📦 Delivery Order Module

Central Kitchen & Franchise Store Backend
GHN (Giao Hàng Nhanh) Integration

📖 Overview

This module integrates GHN (Giao Hàng Nhanh) shipping API into the system to:

Create delivery orders

Validate food availability

Generate internal shipment codes

Store GHN tracking codes

Support COD (Cash on Delivery)

🚀 Features

✅ Create delivery order

✅ Validate food status (AVAILABLE only)

✅ Auto-generate delivery order ID

✅ Integrate GHN shipping API

✅ Save GHN tracking code

🔄 Ready for webhook status updates

🧾 Required Note Options

required_note defines whether the receiver can inspect goods before accepting.

Value	Description
CHOTHUHANG	Allow checking goods
CHOXEMHANGKHONGTHU	Allow checking but not testing
KHONGCHOXEMHANG	Do not allow checking

⚠ Must match exactly (uppercase, no spaces).

💰 Payment Type
Value	Description
1	Sender pays shipping
2	Receiver pays shipping
🚚 Service Type
Value	Description
1	Express delivery
2	Standard delivery
📌 API Endpoint
POST /api/delivery/create
📥 Request Body Example
{
"payment_type_id": 2,
"note": "Deliver during office hours",
"required_note": "CHOTHUHANG",
"to_name": "Le Minh Duc",
"to_phone": "0899306764",
"to_address": "123 Nguyen Trai Street",
"to_ward_code": "20308",
"to_district_id": 1454,
"cod_amount": 250000,
"weight": 1200,
"length": 30,
"width": 20,
"height": 15,
"service_type_id": 2,
"orderDetailId": "OD_001_01",
"foods": {
"CE_CH_FO_130001": 2,
"CE_CH_FO_130002": 1
}
}
🧠 Field Explanation
Receiver Information

to_name → Receiver name

to_phone → Receiver phone number

to_address → Full delivery address

to_ward_code → Ward code from GHN

to_district_id → District ID from GHN

Delivery Information

payment_type_id → Who pays shipping

service_type_id → Express or Standard

required_note → Inspection rule

cod_amount → Amount collected from receiver

Package Information
Field	Unit
weight	grams
length	cm
width	cm
height	cm

⚠ Must be greater than 0.

Foods Structure
"foods": {
"foodId": quantity
}

Example:

"foods": {
"CE_CH_FO_130001": 2,
"CE_CH_FO_130002": 1
}

System will:

Validate food exists

Validate food status = AVAILABLE

Convert to GHN items

Send request to GHN API

🏷 Delivery Order ID Format

Generated automatically:

DO_{PaymentType}_{ServiceType}_{Random6Digits}

Examples:

DO_SE_EX_123456
DO_RE_ST_654321

Where:

SE = Sender pays

RE = Receiver pays

EX = Express

ST = Standard

🔁 Process Flow

Client sends delivery request

System validates food availability

System generates internal shipment code

System calls GHN API

GHN returns order_code

Shipment is saved in database

GHN handles delivery process📦 Delivery Order Module

Central Kitchen & Franchise Store Backend
GHN (Giao Hàng Nhanh) Integration

📖 Overview

This module integrates GHN (Giao Hàng Nhanh) shipping API into the system to:

Create delivery orders

Validate food availability

Generate internal shipment codes

Store GHN tracking codes

Support COD (Cash on Delivery)

🚀 Features

✅ Create delivery order

✅ Validate food status (AVAILABLE only)

✅ Auto-generate delivery order ID

✅ Integrate GHN shipping API

✅ Save GHN tracking code

🔄 Ready for webhook status updates

🧾 Required Note Options

required_note defines whether the receiver can inspect goods before accepting.

Value	Description
CHOTHUHANG	Allow checking goods
CHOXEMHANGKHONGTHU	Allow checking but not testing
KHONGCHOXEMHANG	Do not allow checking

⚠ Must match exactly (uppercase, no spaces).

💰 Payment Type
Value	Description
1	Sender pays shipping
2	Receiver pays shipping
🚚 Service Type
Value	Description
1	Express delivery
2	Standard delivery
📌 API Endpoint
POST /api/delivery/create
📥 Request Body Example
{
"payment_type_id": 2,
"note": "Deliver during office hours",
"required_note": "CHOTHUHANG",
"to_name": "Le Minh Duc",
"to_phone": "0899306764",
"to_address": "123 Nguyen Trai Street",
"to_ward_code": "20308",
"to_district_id": 1454,
"cod_amount": 250000,
"weight": 1200,
"length": 30,
"width": 20,
"height": 15,
"service_type_id": 2,
"orderDetailId": "OD_001_01",
"foods": {
"CE_CH_FO_130001": 2,
"CE_CH_FO_130002": 1
}
}
🧠 Field Explanation
Receiver Information

to_name → Receiver name

to_phone → Receiver phone number

to_address → Full delivery address

to_ward_code → Ward code from GHN

to_district_id → District ID from GHN

Delivery Information

payment_type_id → Who pays shipping

service_type_id → Express or Standard

required_note → Inspection rule

cod_amount → Amount collected from receiver

Package Information
Field	Unit
weight	grams
length	cm
width	cm
height	cm

⚠ Must be greater than 0.

Foods Structure
"foods": {
"foodId": quantity
}

Example:

"foods": {
"CE_CH_FO_130001": 2,
"CE_CH_FO_130002": 1
}

System will:

Validate food exists

Validate food status = AVAILABLE

Convert to GHN items

Send request to GHN API

🏷 Delivery Order ID Format

Generated automatically:

DO_{PaymentType}_{ServiceType}_{Random6Digits}

Examples:

DO_SE_EX_123456
DO_RE_ST_654321

Where:

SE = Sender pays

RE = Receiver pays

EX = Express

ST = Standard

🔁 Process Flow

1. Client sends delivery request

2. System validates food availability

3. System generates internal shipment code

4. System calls GHN API

5. GHN returns order_code

6. Shipment is saved in database

7. GHN handles delivery process