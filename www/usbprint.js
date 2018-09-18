module.exports = {
    print: function(printername, msg, successCallback, failureCallback) {
        cordova.exec(successCallback, failureCallback, "PrintService", "print", [printername, msg]);
    },
    getConnectedPrinters: function(successCallback, failureCallback) {
        cordova.exec(successCallback, failureCallback, "PrintService", "getConnectedPrinters", []);
    },
    isPaperAvailable: function(printername, successCallback, failureCallback) {
        cordova.exec(successCallback, failureCallback, "PrintService", "isPaperAvailable", [printername]);
    },
    cutPaper: function(printername, successCallback, failureCallback) {
        cordova.exec(successCallback, failureCallback, "PrintService", "cutPaper", [printername]);
    },
};