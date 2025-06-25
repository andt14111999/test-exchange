// Image Modal for ActiveAdmin
function openImageModal(imageUrl) {
  // Create modal if it doesn't exist
  let modal = document.getElementById("imageModal");
  if (!modal) {
    modal = document.createElement("div");
    modal.id = "imageModal";
    modal.className = "image-modal";
    modal.innerHTML = `
      <div class="modal-content">
        <span class="close" onclick="closeImageModal()">&times;</span>
        <img class="modal-image" id="modalImage" src="" alt="Receipt Image">
        <div class="modal-caption" id="modalCaption"></div>
      </div>
    `;
    document.body.appendChild(modal);
  }

  // Set image source and show modal
  const modalImage = document.getElementById("modalImage");
  const modalCaption = document.getElementById("modalCaption");

  modalImage.src = imageUrl;
  modalCaption.innerHTML = "Payment Receipt";
  modal.style.display = "block";

  // Close modal when clicking outside
  modal.onclick = function (event) {
    if (event.target === modal) {
      closeImageModal();
    }
  };
}

function closeImageModal() {
  const modal = document.getElementById("imageModal");
  if (modal) {
    modal.style.display = "none";
  }
}

// Close modal with Escape key
document.addEventListener("keydown", function (event) {
  if (event.key === "Escape") {
    closeImageModal();
  }
});
