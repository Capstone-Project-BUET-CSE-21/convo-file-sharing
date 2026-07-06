#!/usr/bin/env bash
# Manual end-to-end smoke test for the metadata/keys endpoints.
# Run this AFTER: mvn spring-boot:run (with your real .env values set)
#
# You need a real session_id to test against, since there's no
# POST /api/sessions endpoint in this manual — insert one directly:
#
#   insert into sessions default values returning session_id;
#
# Copy that UUID and paste it below as SESSION_ID before running this script.

set -e
BASE_URL="http://localhost:8082"

SESSION_ID="6fcf3caa-a69f-4d65-bd8d-e40a6496ded2"
SENDER_ID="$(uuidgen)"

echo "== 1. Register a public key (3.2 POST /api/keys) =="
curl -s -w "\nHTTP %{http_code}\n" -X POST "$BASE_URL/api/keys" \
  -H "Content-Type: application/json" \
  -d "{\"userId\":\"$SENDER_ID\",\"publicKey\":\"BASE64_ENCODED_PUBLIC_KEY_HERE\",\"algorithm\":\"ECDSA-P256\"}"

echo -e "\n== 2. Look up that key (3.2 GET /api/keys/{userId}) =="
curl -s -w "\nHTTP %{http_code}\n" "$BASE_URL/api/keys/$SENDER_ID"

echo -e "\n== 3. Create first transfer in session (3.1 POST) — expect previousHash: null =="
RESPONSE=$(curl -s -X POST "$BASE_URL/api/transfer/metadata" \
  -H "Content-Type: application/json" \
  -d "{\"sessionId\":\"$SESSION_ID\",\"senderId\":\"$SENDER_ID\",\"fileName\":\"first.txt\",\"fileSize\":100,\"mimeType\":\"text/plain\"}")
echo "$RESPONSE"
TRANSFER_ID_1=$(echo "$RESPONSE" | grep -o '"transferId":"[^"]*"' | cut -d'"' -f4)
echo "Captured transferId: $TRANSFER_ID_1"

echo -e "\n== 4. PATCH that transfer with fileHash/signature (3.1 Task 3) =="
curl -s -w "\nHTTP %{http_code}\n" -X PATCH "$BASE_URL/api/transfer/metadata/$TRANSFER_ID_1" \
  -H "Content-Type: application/json" \
  -d '{"fileHash":"deadbeefcafe","signature":"c2lnbmF0dXJlYmFzZTY0"}'

echo -e "\n== 5. PATCH a bogus transferId — expect HTTP 404 =="
curl -s -w "\nHTTP %{http_code}\n" -X PATCH "$BASE_URL/api/transfer/metadata/$(uuidgen)" \
  -H "Content-Type: application/json" \
  -d '{"fileHash":"deadbeef","signature":"c2ln"}'

echo -e "\n== 6. Create a SECOND transfer, same session — expect previousHash populated (not null) =="
curl -s -w "\nHTTP %{http_code}\n" -X POST "$BASE_URL/api/transfer/metadata" \
  -H "Content-Type: application/json" \
  -d "{\"sessionId\":\"$SESSION_ID\",\"senderId\":\"$SENDER_ID\",\"fileName\":\"second.txt\",\"fileSize\":200,\"mimeType\":\"text/plain\"}"

echo -e "\n== 7. Missing required field — expect HTTP 400 =="
curl -s -w "\nHTTP %{http_code}\n" -X POST "$BASE_URL/api/transfer/metadata" \
  -H "Content-Type: application/json" \
  -d "{\"sessionId\":\"$SESSION_ID\",\"senderId\":\"$SENDER_ID\",\"fileSize\":200,\"mimeType\":\"text/plain\"}"

echo -e "\nDone. Cross-check step 3/6 results against the transfer_metadata table in Supabase's Table Editor."
