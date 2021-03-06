package com.kevin.HRSystem.service.impl;

import com.kevin.HRSystem.dao.*;
import com.kevin.HRSystem.exception.HrException;
import com.kevin.HRSystem.model.*;
import com.kevin.HRSystem.service.MgrManagerService;
import com.kevin.HRSystem.vo.ApplicationVo;
import com.kevin.HRSystem.vo.EmployeeVo;
import com.kevin.HRSystem.vo.SalaryVo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

@Service("MgrManagerService")
public class MgrManagerServiceImpl implements MgrManagerService {
    @Resource
    private ApplicationDao applicationDao;
    @Resource
    private AttendTypeDao attendTypeDao;
    @Resource
    private AttendDao attendDao;
    @Resource
    private CheckbackDao checkbackDao;
    @Resource
    private EmployeeDao employeeDao;
    @Resource
    private PaymentDao paymentDao;

    private Manager findByName(String username) {
        Employee tempEmployee = employeeDao.findByName(username);
        if(null != tempEmployee && tempEmployee instanceof Manager) {
            return (Manager)tempEmployee;
        } else {
            return null;
        }
    }

    public void addEmployee(Employee employee, String username) throws HrException {
        Manager manager = findByName(username);
        if(null != manager) {
            employee.setType(1);
            employee.setManager(manager);
            employeeDao.saveAsEmployee(employee);
        } else {
            throw new HrException("您是经理吗？或您还没有登录？");
        }
    }

    //获取上个月的全部工资记录
    public List<SalaryVo> getSalarysByManger(String username) {
        Manager manager = findByName(username);
        if(manager == null) {
            throw new HrException("您是经理吗？或您还没有登录");
        }
        //查询该经理对应的所有员工
        List<Employee> employees = manager.getEmployees();
        if(employees == null || employees.size() < 1) {
            throw new HrException("您的部门没有员工");
        }
        Calendar c =Calendar.getInstance();
        c.add(Calendar.MONTH, -1);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
        String payMonth = sdf.format(c.getTime());
        List<SalaryVo> result = new ArrayList<SalaryVo>();
        for(Employee employee : employees) {
            Payment payment = paymentDao.findByMonthAndEmp(payMonth, employee);
            if(payment != null) {
                result.add(new SalaryVo(employee.getName(), payment.getPayAmount()));
            }
        }
        return result;
    }

    /**
     * 根据经理返回该部门的全部员工
     * @param username 经理名
     * @return
     */
    public List<EmployeeVo> getEmployeesByManager(String username) {
        Manager manager = findByName(username);
        if(manager == null) {
            throw new HrException("您是经理吗？或您未登录");
        }
        List<Employee> employees = manager.getEmployees();
        if(employees == null || employees.size() < 1) {
            throw new HrException("您的部门没有员工");
        }
        List<EmployeeVo> result = new ArrayList<EmployeeVo>();
        for(Employee employee : employees) {
            result.add(new EmployeeVo(employee.getName(), employee.getPassword(), employee.getSalary()));
        }
        return result;
    }

    /**
     * 根据经理返回部门的没有批复的申请
     * @param username 经理名
     * @return
     */
    public List<ApplicationVo> getApplicationsByManager(String username) {
        Manager manager = findByName(username);
        if(manager == null) {
            throw new HrException("您是经理吗？或您还没有登录");
        }
        List<Employee> employees = manager.getEmployees();
        if(employees == null || employees.size() < 1) {
            throw new HrException("您没有员工");
        }
        List<ApplicationVo> result = new ArrayList<ApplicationVo>();
        for(Employee employee : employees) {
            List<Application> applications = applicationDao.findByEmp(employee);
//            System.out.println("employeeName: " + employee.getName());
//            System.out.println("mgrManagerSericeImpl: getApplication: applicatoins:" + applications.size());
            if(applications != null && applications.size() > 0) {
                for(Application application: applications) {
                    if(!application.isApplicationResult()) {
                        Attend attend = application.getAttend();
                        result.add(new ApplicationVo(application.getId(), employee.getName(), attend.getAttendType().getTypeName(), application.getAttendType().getTypeName(), application.getApplicationReason()));
                    }
                }
            }
        }
        return result;
    }

    //处理申请
    public void check(long applicationId, String username, boolean result) {
        Application application = applicationDao.findById(applicationId);
        Checkback checkback = new Checkback();
        checkback.setApplication(application);

        Manager manager = findByName(username);
        if(manager == null) {
            throw new HrException("您是经理吗？或您还没有登录");
        }
        checkback.setManager(manager);
        if(result) {
            checkback.setCheckResult(true);
            //修改申请为已经批复
            application.setApplicationResult(true);
            applicationDao.update(application);
            //修改出勤的类型
            Attend attend = application.getAttend();
            attend.setAttendType(application.getAttendType());
            System.out.println("MgrManagerServiceImpl: ");
            System.out.println(attend);
            attendDao.updateAttendType(attend);
        } else {
            //没有通过申请
            checkback.setCheckResult(false);
            application.setApplicationResult(true);
            applicationDao.update(application);
        }
        System.out.println(checkback);
        checkbackDao.save(checkback);
    }
}
