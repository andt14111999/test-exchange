// Ensure functions are available globally
window.showTradeConfirm = function (action, tradeId, tradeData) {
  const actionText = action === "cancel" ? "Hủy" : "Hoàn thành";
  const actionVerb = action === "cancel" ? "hủy" : "hoàn thành";

  let moneyFlowText = "";
  let warningText = "";

  if (action === "cancel") {
    // Cancel logic: money returns to original holders
    if (tradeData.fiat_token_deposit) {
      moneyFlowText = `
        <div class="money-flow-info">
          <h4>💰 Luồng tiền khi hủy:</h4>
          <p>• <strong>Fiat Token Deposit:</strong> ${
            tradeData.fiat_amount
          } ${tradeData.fiat_currency.toUpperCase()} sẽ được hoàn trả về tài khoản fiat của <strong>Người mua</strong></p>
          <p>• <strong>Crypto:</strong> ${
            tradeData.coin_amount
          } ${tradeData.coin_currency.toUpperCase()} sẽ được giải phóng về tài khoản crypto của <strong>Người bán</strong></p>
        </div>
      `;
    } else if (tradeData.fiat_token_withdrawal) {
      moneyFlowText = `
        <div class="money-flow-info">
          <h4>💰 Luồng tiền khi hủy:</h4>
          <p>• <strong>Fiat Token Withdrawal:</strong> ${
            tradeData.fiat_amount
          } ${tradeData.fiat_currency.toUpperCase()} sẽ được mở khóa về tài khoản fiat của <strong>Người bán</strong></p>
          <p>• <strong>Crypto:</strong> ${
            tradeData.coin_amount
          } ${tradeData.coin_currency.toUpperCase()} sẽ được hoàn trả về tài khoản crypto của <strong>Người mua</strong></p>
        </div>
      `;
    } else {
      // Normal trade
      moneyFlowText = `
        <div class="money-flow-info">
          <h4>💰 Luồng tiền khi hủy:</h4>
          <p>• <strong>Crypto:</strong> ${
            tradeData.coin_amount
          } ${tradeData.coin_currency.toUpperCase()} sẽ được hoàn trả về tài khoản của <strong>Người bán</strong></p>
          <p>• <strong>Fiat:</strong> Người mua tự xử lý thanh toán đã thực hiện (nếu có)</p>
        </div>
      `;
    }
    warningText =
      '<div class="warning-text">⚠️ Hành động này không thể hoàn tác!</div>';
  } else {
    // Release logic: complete the trade
    if (tradeData.fiat_token_deposit) {
      moneyFlowText = `
        <div class="money-flow-info">
          <h4>💰 Luồng tiền khi hoàn thành:</h4>
          <p>• <strong>Fiat Token Deposit:</strong> ${
            tradeData.fiat_amount
          } ${tradeData.fiat_currency.toUpperCase()} sẽ được xử lý và chuyển đến đích</p>
          <p>• <strong>Crypto:</strong> ${
            tradeData.coin_amount
          } ${tradeData.coin_currency.toUpperCase()} sẽ được chuyển cho <strong>Người mua</strong></p>
        </div>
      `;
    } else if (tradeData.fiat_token_withdrawal) {
      moneyFlowText = `
        <div class="money-flow-info">
          <h4>💰 Luồng tiền khi hoàn thành:</h4>
          <p>• <strong>Fiat Token Withdrawal:</strong> ${
            tradeData.fiat_amount
          } ${tradeData.fiat_currency.toUpperCase()} sẽ được rút về tài khoản ngân hàng của <strong>Người bán</strong></p>
          <p>• <strong>Crypto:</strong> ${
            tradeData.coin_amount
          } ${tradeData.coin_currency.toUpperCase()} sẽ được chuyển cho <strong>Người mua</strong></p>
        </div>
      `;
    } else {
      // Normal trade
      moneyFlowText = `
        <div class="money-flow-info">
          <h4>💰 Luồng tiền khi hoàn thành:</h4>
          <p>• <strong>Crypto:</strong> ${
            tradeData.coin_amount
          } ${tradeData.coin_currency.toUpperCase()} sẽ được chuyển cho <strong>Người mua</strong></p>
          <p>• <strong>Fiat:</strong> ${
            tradeData.fiat_amount
          } ${tradeData.fiat_currency.toUpperCase()} đã được thanh toán bởi người mua</p>
        </div>
      `;
    }
    warningText =
      '<div class="success-text">✅ Giao dịch sẽ được hoàn thành và không thể hoàn tác!</div>';
  }

  const modalHtml = `
    <div id="trade-confirm-modal" class="trade-modal-overlay">
      <div class="trade-modal-content">
        <div class="trade-modal-header">
          <h3>${actionText} Giao dịch #${tradeData.ref}</h3>
          <button class="trade-modal-close" onclick="closeTradeConfirm()">&times;</button>
        </div>
        
        <div class="trade-modal-body">
          <div class="trade-info">
            <p><strong>Người mua:</strong> ${tradeData.buyer_name}</p>
            <p><strong>Người bán:</strong> ${tradeData.seller_name}</p>
            <p><strong>Trạng thái hiện tại:</strong> <span class="status-badge status-${
              tradeData.status
            }">${getStatusText(tradeData.status)}</span></p>
          </div>
          
          ${moneyFlowText}
          ${warningText}
          
          <div class="confirmation-question">
            <p><strong>Bạn có chắc chắn muốn ${actionVerb} giao dịch này không?</strong></p>
          </div>
        </div>
        
        <div class="trade-modal-footer">
          <button class="btn btn-secondary" onclick="closeTradeConfirm()">Hủy bỏ</button>
          <button class="btn btn-${
            action === "cancel" ? "danger" : "primary"
          }" onclick="confirmTradeAction('${action}', ${tradeId})">
            ${actionText} Giao dịch
          </button>
        </div>
      </div>
    </div>
  `;

  // Remove existing modal if any
  const existingModal = document.getElementById("trade-confirm-modal");
  if (existingModal) {
    existingModal.remove();
  }

  // Add modal to body
  document.body.insertAdjacentHTML("beforeend", modalHtml);

  // Show modal
  document.getElementById("trade-confirm-modal").style.display = "flex";
};

window.closeTradeConfirm = function () {
  const modal = document.getElementById("trade-confirm-modal");
  if (modal) {
    modal.remove();
  }
};

window.confirmTradeAction = function (action, tradeId) {
  const url =
    action === "cancel"
      ? `/admin/trades/${tradeId}/cancel_trade`
      : `/admin/trades/${tradeId}/release_trade`;

  // Create form and submit
  const form = document.createElement("form");
  form.method = "POST";
  form.action = url;

  // Add CSRF token
  const csrfToken = document
    .querySelector('meta[name="csrf-token"]')
    .getAttribute("content");
  const csrfInput = document.createElement("input");
  csrfInput.type = "hidden";
  csrfInput.name = "authenticity_token";
  csrfInput.value = csrfToken;
  form.appendChild(csrfInput);

  document.body.appendChild(form);
  form.submit();

  closeTradeConfirm();
};

window.getStatusText = function (status) {
  const statusMap = {
    awaiting: "Chờ xử lý",
    unpaid: "Chưa thanh toán",
    paid: "Đã thanh toán",
    disputed: "Tranh chấp",
    released: "Đã hoàn thành",
    cancelled: "Đã hủy",
    cancelled_automatically: "Tự động hủy",
  };
  return statusMap[status] || status;
};

// Close modal when clicking outside
document.addEventListener("click", function (event) {
  const modal = document.getElementById("trade-confirm-modal");
  if (modal && event.target === modal) {
    closeTradeConfirm();
  }
});

// Close modal with ESC key
document.addEventListener("keydown", function (event) {
  if (event.key === "Escape") {
    closeTradeConfirm();
  }
});
