function update() {

    var jobsContainer = $('#jobsContainer');
    var typeFilter = jobsContainer.data("type-filter");
    var jobsUrl = jobsContainer.data("jobs-url");

    $.ajax({
        type: "GET",
        url: jobsUrl + (typeFilter === null ? '' : "?typeFilter=" + typeFilter + "&humanReadable=true"),
        headers: {
            Accept: "application/json; charset=utf-8",
            "Content-Type": "application/json; charset=utf-8"
        },
        data: {},
        dataType: "json",
        error: function (data, status, error) {
            console.log("Error polling job status: " + status + " - " + error);
            setTimeout(update, 10000);
        },
        success: function (data, textStatus, xhr) {
            for (var i in data) {
                var dataRow = null;
                dataRow = data[i];
                var jobStatus = $('#job-status-' + dataRow.id);

                //There is a new job that is not in this list -> reload page!
                if (!jobStatus.length) {
                    location.reload();
                }

                if (dataRow.state !== 'Running') {
                    if (dataRow.status === 'OK') {
                        jobStatus.attr("class", "label label-success");
                        jobStatus.attr("style", "width:10em; height:2em;");
                        jobStatus.html("<span>" + dataRow.status + "</span>");
                    } else if (dataRow.status === 'SKIPPED') {
                        jobStatus.attr("class", "label label-default");
                        jobStatus.attr("style", "width:10em; height:2em;");
                        jobStatus.html("<span>" + dataRow.status + "</span>");
                    } else if (dataRow.status === 'ERROR') {
                        jobStatus.attr("class", "label label-danger");
                        jobStatus.attr("style", "width:10em; height:2em;");
                        jobStatus.html("<span>" + dataRow.status + "</span>");
                    } else if (dataRow.status === 'DEAD') {
                        jobStatus.attr("class", "label label-warning");
                        jobStatus.attr("style", "width:10em; height:2em; background: rgba(230, 110, 30, 1);");
                        jobStatus.html("<span>" + dataRow.status + "</span>");
                    }
                    $("#trigger-button-" + dataRow.id).prop('disabled', false);
                }
                $("#job-stopped-" + dataRow.id).text(dataRow.stopped);
                $("#job-runtime-" + dataRow.id).text(dataRow.runtime);
                //$(".triggerButton").prop('disabled', false);
                $("#job-last-updated-" + dataRow.id).text(dataRow.lastUpdated);
            }
            setTimeout(update, 4000);
        }
    });
}

setTimeout(function () {
    update();
}, 1000);