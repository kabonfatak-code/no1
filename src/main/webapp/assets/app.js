(function () {
    function switchLoginPanel(name) {
        document.querySelectorAll("[data-login-tab]").forEach(function (item) {
            item.classList.toggle("active", item.getAttribute("data-login-tab") === name);
        });
        document.querySelectorAll("[data-login-panel]").forEach(function (panel) {
            panel.classList.toggle("hidden", panel.getAttribute("data-login-panel") !== name);
        });

        var heading = document.querySelector("[data-login-heading]");
        if (heading) {
            heading.textContent = name === "sms" ? "手机号验证码登录" : "用户名密码登录";
        }
    }

    function pageContext() {
        if (typeof window.BBS_CONTEXT === "string") {
            return window.BBS_CONTEXT;
        }
        var path = window.location.pathname;
        var context = "";
        ["/register", "/login", "/password/reset", "/post/detail"].some(function (suffix) {
            if (path.slice(-suffix.length) === suffix) {
                context = path.slice(0, path.length - suffix.length);
                return true;
            }
            return false;
        });
        return context;
    }

    function startSmsCountdown(button, seconds) {
        var originalText = button.getAttribute("data-original-text") || button.textContent;
        button.setAttribute("data-original-text", originalText);
        button.disabled = true;
        var remaining = seconds;
        button.textContent = remaining + " 秒后重试";
        window.clearInterval(button._smsTimer);
        button._smsTimer = window.setInterval(function () {
            remaining -= 1;
            if (remaining <= 0) {
                window.clearInterval(button._smsTimer);
                button.textContent = originalText;
                button.disabled = false;
                return;
            }
            button.textContent = remaining + " 秒后重试";
        }, 1000);
    }

    var forcedLogoutShown = false;

    function handleForcedLogout(data) {
        if (!data || !data.banned) {
            return false;
        }
        if (!forcedLogoutShown) {
            forcedLogoutShown = true;
            alert(data.message || "账号已被封禁，请联系管理员");
        }
        window.location.replace(data.redirect || pageContext() + "/login");
        return true;
    }

    function checkSessionStatus() {
        if (!window.BBS_LOGGED_IN || forcedLogoutShown) {
            return;
        }
        fetch(pageContext() + "/session/status", {
            method: "GET",
            headers: {
                "Accept": "application/json",
                "X-Requested-With": "fetch"
            },
            credentials: "same-origin"
        }).then(function (response) {
            return response.json();
        }).then(function (data) {
            handleForcedLogout(data);
        }).catch(function () {
        });
    }

    function showActionMessage(message, ok) {
        var result = document.getElementById("actionMessage");
        if (!result) {
            return;
        }
        result.textContent = message || "";
        result.classList.toggle("error-message", ok === false);
        result.classList.toggle("visible", Boolean(message));
        window.clearTimeout(result._hideTimer);
        result._hideTimer = window.setTimeout(function () {
            result.classList.remove("visible");
        }, 2200);
    }

    function setStatText(root, selector, value) {
        if (!root || value === undefined || value === null) {
            return;
        }
        var target = root.querySelector(selector);
        if (target) {
            target.textContent = String(value);
        }
    }

    function updatePostStats(post) {
        if (!post || !post.id) {
            return;
        }
        var root = document.querySelector("[data-post-stats][data-post-id='" + post.id + "']");
        setStatText(root, "[data-post-stat='likeScore']", post.likeScore);
        setStatText(root, "[data-post-stat='dislikeScore']", post.dislikeScore);
        setStatText(root, "[data-post-stat='favoriteCount']", post.favoriteCount);
        setStatText(root, "[data-post-stat='commentCount']", post.commentCount);
    }

    function updateCommentStats(comment) {
        if (!comment || !comment.id) {
            return;
        }
        var root = document.querySelector("[data-comment-stats][data-comment-id='" + comment.id + "']");
        setStatText(root, "[data-comment-stat='likeScore']", comment.likeScore);
        setStatText(root, "[data-comment-stat='dislikeScore']", comment.dislikeScore);
    }

    function parseHtml(text) {
        return new DOMParser().parseFromString(text, "text/html");
    }

    function fetchHtml(url) {
        return fetch(url, {
            method: "GET",
            headers: {
                "Accept": "text/html",
                "X-Requested-With": "fetch"
            },
            credentials: "same-origin"
        }).then(function (response) {
            if (!response.ok) {
                throw new Error("HTTP " + response.status);
            }
            return response.text();
        }).then(parseHtml);
    }

    function refreshDetailComments(targetId) {
        var comments = document.getElementById("comments");
        if (!comments) {
            return Promise.resolve();
        }

        return fetchHtml(window.location.href.split("#")[0]).then(function (doc) {
            var freshComments = doc.getElementById("comments");
            if (freshComments) {
                comments.replaceWith(freshComments);
            }

            var freshStats = doc.querySelector("[data-post-stats]");
            var currentStats = document.querySelector("[data-post-stats]");
            if (freshStats && currentStats) {
                currentStats.replaceWith(freshStats);
            }

            if (targetId) {
                var target = document.getElementById(targetId);
                if (target) {
                    var list = target.closest("[data-reply-list]");
                    if (list) {
                        var button = document.querySelector("[data-reply-list-toggle][data-target='" + list.id + "']");
                        setReplyListOpen(button, list, true);
                    }
                    target.scrollIntoView({block: "center"});
                }
            }
        });
    }

    function replaceAdminSections(doc, sectionName) {
        var selector = sectionName ? "[data-admin-section='" + sectionName + "']" : "[data-admin-section]";
        doc.querySelectorAll(selector).forEach(function (freshSection) {
            var name = freshSection.getAttribute("data-admin-section");
            var currentSection = document.querySelector("[data-admin-section='" + name + "']");
            if (currentSection) {
                currentSection.replaceWith(freshSection);
            }
        });
    }

    function refreshAdmin(url, sectionName, pushState) {
        return fetchHtml(url).then(function (doc) {
            replaceAdminSections(doc, sectionName);
            if (pushState) {
                window.history.pushState({bbsAdmin: true}, "", url);
            }
        });
    }

    document.addEventListener("click", function (event) {
        var replyListButton = event.target.closest("[data-reply-list-toggle]");
        if (replyListButton) {
            event.preventDefault();
            var list = document.getElementById(replyListButton.getAttribute("data-target"));
            if (list) {
                var willOpen = list.classList.contains("collapsed");
                setReplyListOpen(replyListButton, list, willOpen);
            }
            return;
        }

        var disclosureButton = event.target.closest("[data-disclosure-toggle]");
        if (disclosureButton) {
            event.preventDefault();
            var disclosureForm = disclosureButton.closest("[data-disclosure-form]");
            if (disclosureForm) {
                var fields = disclosureForm.querySelector("[data-disclosure-fields]");
                if (fields) {
                    var willOpen = fields.classList.contains("hidden");
                    fields.classList.toggle("hidden", !willOpen);
                    disclosureForm.classList.toggle("is-expanded", willOpen);
                    if (willOpen) {
                        var firstField = fields.querySelector("input, textarea");
                        if (firstField) {
                            firstField.focus();
                        }
                    }
                }
            }
            return;
        }

        var cancelButton = event.target.closest("[data-disclosure-cancel]");
        if (cancelButton) {
            event.preventDefault();
            var cancelForm = cancelButton.closest("[data-disclosure-form]");
            if (cancelForm) {
                var cancelFields = cancelForm.querySelector("[data-disclosure-fields]");
                if (cancelFields) {
                    cancelFields.classList.add("hidden");
                }
                cancelForm.classList.remove("is-expanded");
            }
            return;
        }

        var switchButton = event.target.closest("[data-login-switch]");
        if (switchButton) {
            switchLoginPanel(switchButton.getAttribute("data-login-switch"));
            return;
        }

        var tab = event.target.closest("[data-login-tab]");
        if (tab) {
            switchLoginPanel(tab.getAttribute("data-login-tab"));
            return;
        }

        var button = event.target.closest("[data-sms-purpose]");
        if (!button) {
            var pageLink = event.target.closest("[data-admin-section] .pagination a.page-link");
            if (pageLink) {
                event.preventDefault();
                var adminSection = pageLink.closest("[data-admin-section]");
                var sectionName = adminSection ? adminSection.getAttribute("data-admin-section") : null;
                refreshAdmin(pageLink.href, sectionName, true).catch(function () {
                    window.location.href = pageLink.href;
                });
            }
            return;
        }

        var phoneInput = document.getElementById(button.getAttribute("data-phone-input"));
        var result = document.getElementById("smsResult");
        var phone = phoneInput ? phoneInput.value.trim() : "";
        if (!phone) {
            if (result) {
                result.textContent = "请先输入电话";
            }
            return;
        }

        button.disabled = true;
        var form = new URLSearchParams();
        form.set("phone", phone);
        form.set("purpose", button.getAttribute("data-sms-purpose"));

        fetch(pageContext() + "/sms-code", {
            method: "POST",
            headers: {"Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"},
            body: form.toString(),
            credentials: "same-origin"
        }).then(function (response) {
            return response.json();
        }).then(function (data) {
            if (handleForcedLogout(data)) {
                return;
            }
            if (result) {
                result.textContent = data.message;
            }
            if (data.ok) {
                startSmsCountdown(button, 60);
            } else {
                button.disabled = false;
            }
        }).catch(function () {
            if (result) {
                result.textContent = "验证码获取失败";
            }
            button.disabled = false;
        });
    });

    document.addEventListener("submit", function (event) {
        var adminForm = event.target.closest("form.ajax-admin-action");
        if (adminForm) {
            event.preventDefault();
            var adminSubmitter = event.submitter;
            var adminBody = new URLSearchParams(new FormData(adminForm));
            if (adminSubmitter && adminSubmitter.name) {
                adminBody.set(adminSubmitter.name, adminSubmitter.value);
            }
            if (adminSubmitter) {
                adminSubmitter.disabled = true;
            }

            fetch(new URL(adminForm.getAttribute("action"), window.location.href).toString(), {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8",
                    "X-Requested-With": "fetch"
                },
                body: adminBody.toString(),
                credentials: "same-origin"
            }).then(function (response) {
                return response.json();
            }).then(function (data) {
                showActionMessage(data.message, data.ok);
                if (data.ok) {
                    return refreshAdmin(window.location.href, null, false).catch(function () {
                        showActionMessage("操作已完成，请手动刷新查看最新内容", false);
                    });
                }
                return null;
            }).catch(function () {
                showActionMessage("操作失败，请稍后再试", false);
            }).finally(function () {
                if (adminSubmitter) {
                    adminSubmitter.disabled = false;
                }
            });
            return;
        }

        var form = event.target.closest("form.ajax-action");
        if (!form) {
            return;
        }

        event.preventDefault();
        var submitter = event.submitter;
        var body = new URLSearchParams(new FormData(form));
        if (submitter && submitter.name) {
            body.set(submitter.name, submitter.value);
        }

        if (submitter) {
            submitter.disabled = true;
        }

        var actionUrl = new URL(form.getAttribute("action"), window.location.href).toString();

        fetch(actionUrl, {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8",
                "X-Requested-With": "fetch"
            },
            body: body.toString(),
            credentials: "same-origin"
        }).then(function (response) {
            return response.json();
        }).then(function (data) {
            if (handleForcedLogout(data)) {
                return;
            }
            showActionMessage(data.message, data.ok);
            if (data.ok) {
                updatePostStats(data.post);
                updateCommentStats(data.comment);

                var action = body.get("action");
                var isCommentAction = actionUrl.indexOf("/comment/action") !== -1;
                if (action === "report") {
                    var reason = form.querySelector("input[name='reason']");
                    if (reason) {
                        reason.value = "";
                    }
                    var reportFields = form.querySelector("[data-disclosure-fields]");
                    if (reportFields) {
                        reportFields.classList.add("hidden");
                        form.classList.remove("is-expanded");
                    }
                } else if (action === "edit" && form.classList.contains("comment-edit")) {
                    var textarea = form.querySelector("textarea[name='content']");
                    var comment = form.closest(".comment");
                    var content = comment ? comment.querySelector(".comment-content") : null;
                    if (textarea && content) {
                        content.textContent = textarea.value;
                    }
                    var editFields = form.querySelector("[data-disclosure-fields]");
                    if (editFields) {
                        editFields.classList.add("hidden");
                        form.classList.remove("is-expanded");
                    }
                } else if (action === "add") {
                    var addTextarea = form.querySelector("textarea[name='content']");
                    if (addTextarea) {
                        addTextarea.value = "";
                    }
                    var addFields = form.querySelector("[data-disclosure-fields]");
                    if (addFields) {
                        addFields.classList.add("hidden");
                        form.classList.remove("is-expanded");
                    }
                }
                if (isCommentAction && (action === "add" || action === "edit" || action === "delete")) {
                    var targetId = data.comment && data.comment.id ? "comment-" + data.comment.id : "comments";
                    return refreshDetailComments(targetId).catch(function () {
                        showActionMessage("操作已完成，请手动刷新查看最新内容", false);
                    });
                }
            }
        }).catch(function () {
            showActionMessage("操作失败，请稍后再试", false);
        }).finally(function () {
            if (submitter) {
                submitter.disabled = false;
            }
        });
    });

    function setReplyListOpen(button, list, open) {
        list.classList.toggle("collapsed", !open);
        if (button) {
            button.setAttribute("aria-expanded", open ? "true" : "false");
            button.textContent = open
                ? button.getAttribute("data-open-text")
                : button.getAttribute("data-closed-text");
        }
    }

    function expandReplyListForHash() {
        if (!window.location.hash) {
            return;
        }
        var target = document.getElementById(window.location.hash.slice(1));
        if (!target) {
            return;
        }
        var list = target.closest("[data-reply-list]");
        if (!list) {
            return;
        }
        var button = document.querySelector("[data-reply-list-toggle][data-target='" + list.id + "']");
        setReplyListOpen(button, list, true);
        window.setTimeout(function () {
            target.scrollIntoView({block: "center"});
        }, 0);
    }

    if (window.BBS_LOGGED_IN) {
        window.setTimeout(checkSessionStatus, 5000);
        window.setInterval(checkSessionStatus, 15000);
    }
    window.addEventListener("popstate", function () {
        if (document.querySelector("[data-admin-section]")) {
            refreshAdmin(window.location.href, null, false).catch(function () {
                window.location.reload();
            });
        }
    });
    expandReplyListForHash();
})();
