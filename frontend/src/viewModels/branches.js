/** Branch, employee and access administration with permission-gated writes. */
define([
  'knockout', '../services/endpoints', '../services/format', '../services/http', '../services/session', '../services/locations',
  './support/banner', './support/confirm', 'ojs/ojdialog'
], function (ko, endpoints, fmt, http, session, locations, Banner, Confirm) {
  'use strict';

  var DAYS = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'];

  function BranchesViewModel() {
    var self = this;
    Banner.call(self);
    Confirm.call(self, { dialogId: 'organizationAdminDialog' });

    self.loading = ko.observable(true);
    self.detailsLoading = ko.observable(false);
    self.error = ko.observable('');
    self.detailError = ko.observable('');
    self.activeTab = ko.observable('branches');
    self.branches = ko.observableArray([]);
    self.employees = ko.observableArray([]);
    self.users = ko.observableArray([]);
    self.roles = ko.observableArray([]);
    self.permissions = ko.observableArray([]);
    self.selectedBranch = ko.observable(null);
    self.selectedEmployee = ko.observable(null);
    self.selectedRole = ko.observable(null);
    self.workingHours = ko.observableArray([]);
    self.holidays = ko.observableArray([]);
    self.authorities = ko.observableArray([]);

    self.canManageBranches = session.hasPermission('BRANCH_MANAGE');
    self.canManageEmployees = session.hasPermission('EMPLOYEE_MANAGE');
    self.canManageUsers = session.hasPermission('USER_MANAGE');
    self.canManageRoles = session.hasPermission('ROLE_PERMISSION_MANAGE');
    self.canCreateEmployee = self.canManageEmployees && self.canManageUsers;
    self.employeeStatuses = ['ACTIVE', 'ON_LEAVE', 'RESIGNED'];

    self.formError = ko.observable('');
    self.formBranchCode = ko.observable('');
    self.formBranchName = ko.observable('');
    self.formAddress = ko.observable('');
    self.formCity = ko.observable('');
    self.formState = ko.observable('');
    self.formPincode = ko.observable('');
    self.stateOptions = locations.states;
    self.cityOptions = ko.pureComputed(function () {
      var choices = locations.citiesFor(self.formState()).slice();
      var current = self.formCity();
      if (current && choices.indexOf(current) === -1) { choices.unshift(current); }
      return choices;
    });
    self.formIfsc = ko.observable('');
    self.formHolidayDate = ko.observable('');
    self.formHolidayDescription = ko.observable('');
    self.hoursRows = ko.observableArray([]);
    self.formUsername = ko.observable('');
    self.formEmail = ko.observable('');
    self.formPassword = ko.observable('');
    self.formFullName = ko.observable('');
    self.formMobile = ko.observable('');
    self.formEmployeeCode = ko.observable('');
    self.formEmployeeDob = ko.observable('');
    self.formBranchId = ko.observable('');
    self.formDesignation = ko.observable('');
    self.formManagerId = ko.observable('');
    self.formJoiningDate = ko.observable('');
    self.formEmployeeStatus = ko.observable('ACTIVE');
    self.formRoleName = ko.observable('TELLER');
    self.formTransferBranchId = ko.observable('');
    self.formTransferRemarks = ko.observable('');
    self.formAuthorityAccount = ko.observable('');
    self.formAuthorityTransaction = ko.observable('');
    self.formAuthorityReversal = ko.observable('');
    self.createdUserId = ko.observable(null);
    self.formRoleCode = ko.observable('');
    self.formRoleDescription = ko.observable('');
    self.selectedPermissionIds = ko.observableArray([]);

    self.activeCount = ko.pureComputed(function () {
      return self.branches().filter(function (row) { return row.status === 'ACTIVE'; }).length;
    });
    self.selectedEmployees = ko.pureComputed(function () {
      var branch = self.selectedBranch();
      return branch ? self.employees().filter(function (row) { return row.branchId === branch.id; }) : [];
    });
    self.staffRoles = ko.pureComputed(function () {
      return self.roles().filter(function (row) { return row.roleName !== 'CUSTOMER'; });
    });
    self.managerChoices = ko.pureComputed(function () {
      var selected = self.selectedEmployee();
      return self.employees().filter(function (row) { return !selected || row.id !== selected.id; });
    });
    self.dialogAction = ko.pureComputed(function () {
      var payload = self.confirmPayload();
      return payload ? payload.action : '';
    });
    self.dialogTitle = ko.pureComputed(function () {
      var labels = {
        createBranch: 'Create branch', editBranch: 'Edit branch', branchStatus: 'Change branch status',
        hours: 'Working hours', holiday: 'Add holiday', createEmployee: 'Create staff login and employee',
        editEmployee: 'Edit employee', transferEmployee: 'Transfer employee', authority: 'Approval authority',
        createRole: 'Create role', editRole: 'Edit role and permissions'
      };
      return labels[self.dialogAction()] || 'Organization administration';
    });
    self.confirmLabel = ko.pureComputed(function () {
      if (self.busy()) { return 'Saving…'; }
      var labels = { createBranch: 'Create branch', createEmployee: 'Create employee', createRole: 'Create role',
        branchStatus: 'Confirm status', transferEmployee: 'Transfer employee', holiday: 'Add holiday' };
      return labels[self.dialogAction()] || 'Save changes';
    });

    function userFor(userId) {
      return self.users().find(function (user) { return user.userId === userId; }) || null;
    }
    function branchFor(branchId) {
      return self.branches().find(function (branch) { return branch.id === branchId; }) || null;
    }
    function decorateEmployee(row) {
      var user = userFor(row.userId);
      var branch = branchFor(row.branchId);
      row.joinedDisplay = fmt.dateOnly(row.joiningDate);
      row.statusClass = 'mb-pill mb-pill--' + fmt.toneFor(row.status);
      row.fullName = user ? user.fullName : 'User ' + row.userId;
      row.username = user ? user.username : '';
      row.loginStatus = user ? user.status : '';
      row.roleName = user && user.roles && user.roles.length ? user.roles[0] : '';
      row.branchCode = branch ? branch.branchCode : String(row.branchId);
      row.managerName = row.reportingManagerId ? 'Employee ' + row.reportingManagerId : 'No manager';
      row.inspect = function () { self.inspectEmployee(row); };
      row.closeDetail = function () { self.selectedEmployee(null); self.authorities([]); };
      row.canManage = self.canManageEmployees;
      row.edit = function () { self.beginEditEmployee(); };
      row.transfer = function () { self.beginTransferEmployee(); };
      row.editAuthority = function () { self.beginAuthority(); };
      return row;
    }
    function decorateBranch(row, employees) {
      row.locationDisplay = [row.city, row.state, row.pincode].filter(Boolean).join(', ');
      row.statusClass = 'mb-pill mb-pill--' + fmt.toneFor(row.status);
      row.employeeCount = employees.filter(function (employee) { return employee.branchId === row.id; }).length;
      row.inspect = function () { self.inspectBranch(row); };
      row.closeDetail = function () { self.selectedBranch(null); };
      row.canManage = self.canManageBranches;
      row.edit = function () { self.beginEditBranch(); };
      row.editHours = function () { self.beginHours(); };
      row.addHoliday = function () { self.beginHoliday(); };
      row.changeStatus = function () { self.beginBranchStatus(); };
      return row;
    }
    function decorateRole(row) {
      row.permissionCount = (row.permissions || []).length;
      row.inspect = function () { self.selectedRole(row); };
      row.closeDetail = function () { self.selectedRole(null); };
      row.editRole = function () { self.beginEditRole(); };
      return row;
    }

    self.showTab = function (tab) {
      self.activeTab(tab);
      self.selectedBranch(null); self.selectedEmployee(null); self.selectedRole(null);
    };

    self.load = function () {
      self.loading(true); self.error('');
      var calls = [endpoints.branches.list(), endpoints.branches.employees()];
      calls.push(self.canManageUsers ? endpoints.identity.users({ size: 100 }) : Promise.resolve({ items: [] }));
      calls.push(self.canManageRoles || self.canManageUsers ? endpoints.identity.roles() : Promise.resolve([]));
      calls.push(self.canManageRoles ? endpoints.identity.permissions() : Promise.resolve([]));
      return Promise.all(calls).then(function (parts) {
        self.users((parts[2] && parts[2].items) || []);
        self.roles((parts[3] || []).map(decorateRole));
        self.permissions((parts[4] || []).map(function (permission) {
          permission.idString = String(permission.permissionId);
          return permission;
        }));
        var rawEmployees = parts[1] || [];
        self.branches((parts[0] || []).map(function (row) { return decorateBranch(row, rawEmployees); }));
        self.employees(rawEmployees.map(decorateEmployee));
      }).catch(function (err) {
        if (!err || !err.isSessionExpired) { self.error(http.messageFor(err)); }
      }).then(function () { self.loading(false); });
    };

    self.inspectBranch = function (row) {
      self.selectedBranch(row); self.workingHours([]); self.holidays([]); self.detailError(''); self.detailsLoading(true);
      Promise.all([endpoints.branches.workingHours(row.id), endpoints.branches.holidays(row.id)])
        .then(function (parts) {
          self.workingHours((parts[0] || []).map(function (day) {
            day.dayDisplay = fmt.humanize(day.dayOfWeek || day.day);
            day.closed = day.isClosed === 'Y' || day.closed === true;
            day.hoursDisplay = day.closed ? 'Closed' : (day.openTime || '—') + ' – ' + (day.closeTime || '—');
            return day;
          }));
          self.holidays((parts[1] || []).map(function (holiday) {
            holiday.dateDisplay = fmt.dateOnly(holiday.holidayDate || holiday.date);
            holiday.nameDisplay = holiday.description || holiday.name || holiday.holidayName || 'Holiday';
            holiday.canRemove = self.canManageBranches;
            holiday.remove = function () { self.beginDeleteHoliday(holiday); };
            return holiday;
          }));
        }).catch(function (err) {
          if (!err || !err.isSessionExpired) { self.detailError(http.messageFor(err)); }
        }).then(function () { self.detailsLoading(false); });
    };

    self.inspectEmployee = function (row) {
      self.selectedEmployee(row); self.authorities([]); self.detailError(''); self.detailsLoading(true);
      endpoints.branches.approvalAuthority(row.id).then(function (rows) {
        self.authorities((rows || []).map(function (authority) {
          authority.amountDisplay = fmt.money(authority.maxAmount, authority.currency);
          authority.actionDisplay = fmt.humanize(authority.actionType);
          return authority;
        }));
      }).catch(function (err) {
        if (!err || !err.isSessionExpired) { self.detailError(http.messageFor(err)); }
      }).then(function () { self.detailsLoading(false); });
    };

    function clearForm() {
      self.formError(''); self.createdUserId(null);
      self.formBranchCode(''); self.formBranchName(''); self.formAddress(''); self.formState(''); self.formCity(''); self.formPincode(''); self.formIfsc('');
      self.formHolidayDate(''); self.formHolidayDescription(''); self.hoursRows([]);
      self.formUsername(''); self.formEmail(''); self.formPassword(''); self.formFullName(''); self.formMobile(''); self.formEmployeeCode(''); self.formEmployeeDob('');
      self.formBranchId(''); self.formDesignation(''); self.formManagerId(''); self.formJoiningDate(''); self.formEmployeeStatus('ACTIVE'); self.formRoleName('TELLER');
      self.formTransferBranchId(''); self.formTransferRemarks(''); self.formAuthorityAccount(''); self.formAuthorityTransaction(''); self.formAuthorityReversal('');
      self.formRoleCode(''); self.formRoleDescription(''); self.selectedPermissionIds([]);
    }
    function open(action, row) { self.openConfirm({ action: action, row: row || null }); }
    function textValue(observable, label) {
      var result = String(observable() || '').trim();
      if (!result) { throw new Error(label + ' is required.'); }
      return result;
    }
    function optionalId(observable) {
      var value = String(observable() || '').trim();
      return value ? Number(value) : null;
    }

    self.beginCreateBranch = function () { clearForm(); open('createBranch'); };
    self.beginEditBranch = function () {
      var row = self.selectedBranch(); clearForm();
      self.formBranchName(row.name || ''); self.formAddress(row.address || ''); self.formState(row.state || ''); self.formCity(row.city || ''); self.formPincode(row.pincode || '');
      open('editBranch', row);
    };
    self.beginBranchStatus = function () { clearForm(); open('branchStatus', self.selectedBranch()); };
    self.beginHours = function () {
      var current = self.workingHours(); clearForm();
      self.hoursRows(DAYS.map(function (day) {
        var found = current.find(function (row) { return (row.dayOfWeek || row.day) === day; }) || {};
        return { dayOfWeek: day, dayDisplay: fmt.humanize(day), openTime: ko.observable(found.openTime || '09:00'), closeTime: ko.observable(found.closeTime || '17:00'), closed: ko.observable(!!found.closed) };
      }));
      open('hours', self.selectedBranch());
    };
    self.beginHoliday = function () { clearForm(); open('holiday', self.selectedBranch()); };
    self.beginDeleteHoliday = function (holiday) {
      var branch = self.selectedBranch();
      if (!branch || !window.confirm('Remove ' + holiday.nameDisplay + ' from ' + branch.branchCode + '?')) { return; }
      endpoints.branches.deleteHoliday(branch.id, holiday.id).then(function () {
        self.notify('success', 'Holiday removed', holiday.nameDisplay + ' was removed.'); self.inspectBranch(branch);
      }).catch(function (err) { self.failed('Holiday could not be removed', err); });
    };
    self.beginCreateEmployee = function () { clearForm(); open('createEmployee'); };
    self.beginEditEmployee = function () {
      var row = self.selectedEmployee(); clearForm();
      self.formEmployeeDob(row.dob || ''); self.formDesignation(row.designation || ''); self.formManagerId(row.reportingManagerId || ''); self.formEmployeeStatus(row.status || 'ACTIVE'); self.formRoleName(row.roleName || 'TELLER');
      open('editEmployee', row);
    };
    self.beginTransferEmployee = function () { clearForm(); open('transferEmployee', self.selectedEmployee()); };
    self.beginAuthority = function () {
      var row = self.selectedEmployee(); var values = {}; self.authorities().forEach(function (a) { values[a.actionType] = a.maxAmount; }); clearForm();
      self.formAuthorityAccount(values.ACCOUNT_APPROVE || ''); self.formAuthorityTransaction(values.TRANSACTION_APPROVE || ''); self.formAuthorityReversal(values.TRANSACTION_REVERSE || '');
      open('authority', row);
    };
    self.beginCreateRole = function () { clearForm(); open('createRole'); };
    self.beginEditRole = function () {
      var row = self.selectedRole(); clearForm(); self.formRoleCode(row.roleName); self.formRoleDescription(row.description || '');
      var codes = row.permissions || [];
      self.selectedPermissionIds(self.permissions().filter(function (p) { return codes.indexOf(p.permissionCode) !== -1; }).map(function (p) { return p.idString; }));
      open('editRole', row);
    };

    function branchBody(create) {
      var body = { name: textValue(self.formBranchName, 'Branch name'), address: (self.formAddress() || '').trim() || null, city: (self.formCity() || '').trim() || null, state: (self.formState() || '').trim() || null, pincode: (self.formPincode() || '').trim() || null };
      if (create) { body.branchCode = textValue(self.formBranchCode, 'Branch code').toUpperCase(); body.ifscCode = textValue(self.formIfsc, 'IFSC').toUpperCase(); }
      return body;
    }
    function employeeCreateBodies() {
      var branchId = Number(textValue(self.formBranchId, 'Branch'));
      var branch = branchFor(branchId);
      if (!branch) { throw new Error('Choose a valid branch.'); }
      var password = textValue(self.formPassword, 'Temporary password');
      if (password.length < 8) { throw new Error('Temporary password must contain at least 8 characters.'); }
      var employeeCode = textValue(self.formEmployeeCode, 'Employee code').toUpperCase();
      if (self.employees().some(function (employee) { return String(employee.employeeCode || '').toUpperCase() === employeeCode; })) {
        throw new Error('Employee code ' + employeeCode + ' is already in use.');
      }
      return {
        user: { username: textValue(self.formUsername, 'Username'), email: textValue(self.formEmail, 'Email'), password: password, fullName: textValue(self.formFullName, 'Full name'), mobile: self.formMobile().trim() || null, branchCode: branch.branchCode, roles: [self.formRoleName()] },
        employee: { employeeCode: employeeCode, dob: self.formEmployeeDob() || null, branchId: branchId, designation: textValue(self.formDesignation, 'Designation'), reportingManagerId: optionalId(self.formManagerId), joiningDate: self.formJoiningDate() || null }
      };
    }
    function authorityBody() {
      var rows = [];
      [[self.formAuthorityAccount, 'ACCOUNT_APPROVE'], [self.formAuthorityTransaction, 'TRANSACTION_APPROVE'], [self.formAuthorityReversal, 'TRANSACTION_REVERSE']].forEach(function (item) {
        var raw = String(item[0]() || '').trim();
        if (raw) {
          var amount = Number(raw);
          if (!isFinite(amount) || amount <= 0) { throw new Error(fmt.humanize(item[1]) + ' must be greater than zero.'); }
          rows.push({ actionType: item[1], maxAmount: amount, currency: 'INR' });
        }
      });
      return rows;
    }

    self.confirm = function () {
      var payload = self.confirmPayload(); if (!payload) { return; }
      var action = payload.action; var prepared;
      try {
        if (action === 'createBranch') { prepared = branchBody(true); }
        else if (action === 'editBranch') { prepared = branchBody(false); }
        else if (action === 'holiday') { prepared = { holidayDate: textValue(self.formHolidayDate, 'Holiday date'), description: textValue(self.formHolidayDescription, 'Description') }; }
        else if (action === 'hours') { prepared = self.hoursRows().map(function (day) { return { dayOfWeek: day.dayOfWeek, openTime: day.closed() ? null : day.openTime(), closeTime: day.closed() ? null : day.closeTime(), closed: day.closed() }; }); }
        else if (action === 'createEmployee') { prepared = employeeCreateBodies(); }
        else if (action === 'editEmployee') { prepared = { dob: self.formEmployeeDob() || null, designation: textValue(self.formDesignation, 'Designation'), reportingManagerId: optionalId(self.formManagerId), status: self.formEmployeeStatus(), role: self.formRoleName() }; }
        else if (action === 'transferEmployee') { prepared = { toBranchId: Number(textValue(self.formTransferBranchId, 'Destination branch')), remarks: self.formTransferRemarks().trim() || null }; }
        else if (action === 'authority') { prepared = authorityBody(); }
        else if (action === 'createRole' || action === 'editRole') { prepared = { roleName: textValue(self.formRoleCode, 'Role code').toUpperCase(), description: self.formRoleDescription().trim() || null, permissionIds: self.selectedPermissionIds().map(Number) }; }
      } catch (validationError) { self.formError(validationError.message); return; }
      self.formError('');
      self.runConfirm(function (committed, intent) {
        var row = committed.row;
        if (action === 'createBranch') { return endpoints.branches.create(prepared, intent.idempotencyKey); }
        if (action === 'editBranch') { return endpoints.branches.update(row.id, prepared, intent.idempotencyKey); }
        if (action === 'branchStatus') { return row.status === 'ACTIVE' ? endpoints.branches.deactivate(row.id, intent.idempotencyKey) : endpoints.branches.activate(row.id, intent.idempotencyKey); }
        if (action === 'holiday') { return endpoints.branches.addHoliday(row.id, prepared, intent.idempotencyKey); }
        if (action === 'hours') { return endpoints.branches.replaceWorkingHours(row.id, prepared, intent.idempotencyKey); }
        if (action === 'createEmployee') {
          return endpoints.identity.createUser(prepared.user, intent.idempotencyKey).then(function (user) {
            self.createdUserId(user.userId); prepared.employee.userId = user.userId;
            return endpoints.branches.createEmployee(prepared.employee, intent.idempotencyKey);
          });
        }
        if (action === 'editEmployee') {
          var roleChanged = prepared.role && prepared.role !== row.roleName;
          var employeeBody = { dob: prepared.dob, designation: prepared.designation, status: prepared.status };
          return endpoints.branches.updateEmployee(row.id, employeeBody, intent.idempotencyKey).then(function (employee) {
            return endpoints.branches.updateManager(row.id, prepared.reportingManagerId, intent.idempotencyKey).then(function () { return employee; });
          }).then(function (employee) {
            return roleChanged ? endpoints.identity.replaceUserRole(row.userId, prepared.role, intent.idempotencyKey).then(function () { return employee; }) : employee;
          }).then(function (employee) {
            if (prepared.status === 'RESIGNED' && row.loginStatus !== 'DISABLED') {
              return endpoints.identity.disableUser(row.userId, intent.idempotencyKey).then(function () { return employee; });
            }
            if (prepared.status === 'ACTIVE' && row.loginStatus === 'DISABLED') {
              return endpoints.identity.enableUser(row.userId, intent.idempotencyKey).then(function () { return employee; });
            }
            return employee;
          });
        }
        if (action === 'transferEmployee') { return endpoints.branches.transferEmployee(row.id, prepared, intent.idempotencyKey); }
        if (action === 'authority') { return endpoints.branches.replaceApprovalAuthority(row.id, prepared, intent.idempotencyKey); }
        if (action === 'createRole') {
          return endpoints.identity.createRole({ roleName: prepared.roleName, description: prepared.description }, intent.idempotencyKey).then(function (role) {
            return endpoints.identity.replaceRolePermissions(role.roleId, prepared.permissionIds, intent.idempotencyKey);
          });
        }
        if (action === 'editRole') {
          return endpoints.identity.updateRole(row.roleId, { roleName: prepared.roleName, description: prepared.description }, intent.idempotencyKey).then(function () {
            return endpoints.identity.replaceRolePermissions(row.roleId, prepared.permissionIds, intent.idempotencyKey);
          });
        }
      }).then(function (result) {
        if (result === null) { return; }
        self.notify('success', 'Organization updated', self.dialogTitle() + ' completed successfully.');
        self.selectedBranch(null); self.selectedEmployee(null); self.selectedRole(null); return self.load();
      }).catch(function (err) {
        var userId = self.createdUserId();
        self.notify('error', 'Organization update failed', (userId ? 'Login user ' + userId + ' was created; do not create it again. ' : '') + http.messageFor(err));
      });
    };

    self.load();
  }

  return BranchesViewModel;
});
