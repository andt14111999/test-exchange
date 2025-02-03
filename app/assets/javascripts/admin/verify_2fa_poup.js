function verify2FaAndProcess(actionUrl) {
  const userCode = prompt("Please enter the verification code:");

  if (userCode) {
    fetch(actionUrl, {
      method: "POST",
      headers: {
        "X-CSRF-Token": document.querySelector('meta[name="csrf-token"]')
          .content,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        code: userCode,
      }),
    }).then(() => {
      window.location.reload();
    });
  } else {
    alert("Please enter the verification 2Fa code");
  }
}
