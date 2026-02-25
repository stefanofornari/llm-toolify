function log(record) {
    n = document.getElementById("tree").children.length;
    element = document.createElement("div");
    element.id = `record-${++n}`;
    document.getElementById("tree").append(element);
    jsonview.render(jsonview.create(record), element);

    const num = n.toString().padStart(4, '0');
    updateRecordLabel(element.id, num);
}

function clear() {
    document.getElementById("tree").replaceChildren();
}

function updateRecordLabel(recordId, newLabel) {
    const keyElement = document.querySelector(`#${recordId} .json-container .line .json-key`);
    if (keyElement) {
        keyElement.textContent = newLabel;
    }
}

function highlight(divToHighlight) {
    alert("highlighting " + divToHighlight)
    // Remove highlight from all records
    document.querySelectorAll('[id^="record-"]').forEach(div => {
        div.classList.remove("highlighted");
    });

    // Highlight the clicked record
    divToHighlight.classList.add("highlighted");
}

function onClickSetup() {
    const treeContainer = document.getElementById("tree");

    //
    // Event delegation: listen for clicks on record divs
    // One event handler for all divs for efficiency
    //
    treeContainer.addEventListener("click", function(event) {
        const recordDiv = event.target.closest('[id^="record-"]');

        if (recordDiv) {
            highlight(recordDiv);
            if (window.mainController) {
                const params = new URLSearchParams(window.location.search);
                const role = params.get('role');
                window.mainController.onLogClick(role, recordDiv.id);
            }
        }
    });
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', onClickSetup);
} else {
    onClickSetup();  // DOM already loaded
}