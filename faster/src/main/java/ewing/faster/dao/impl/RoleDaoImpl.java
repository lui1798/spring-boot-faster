package ewing.faster.dao.impl;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.QBean;
import com.querydsl.sql.SQLQuery;
import ewing.faster.application.config.FasterBasisDao;
import ewing.faster.dao.RoleDao;
import ewing.faster.dao.entity.Authority;
import ewing.faster.dao.entity.Role;
import ewing.faster.dao.query.QRole;
import ewing.faster.security.vo.RoleWithAuthority;
import ewing.query.paging.Page;
import ewing.query.paging.Pager;
import ewing.query.QueryUtils;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.List;

/**
 * 角色数据访问实现。
 */
@Repository
public class RoleDaoImpl extends FasterBasisDao<QRole, Role> implements RoleDao {

    private QBean<RoleWithAuthority> qRoleWithAuthority = Projections
            .bean(RoleWithAuthority.class, qRole.all());

    @Override
    public Page<RoleWithAuthority> findRoleWithAuthority(Pager pager, Predicate predicate) {
        // 查询角色总数
        SQLQuery<Role> roleQuery = getQueryFactory().selectFrom(qRole)
                .where(predicate);
        long total = roleQuery.fetchCount();

        // 分页查询并附带权限
        roleQuery.limit(pager.getLimit()).offset(pager.getOffset());
        List<Tuple> rows = getQueryFactory().select(qRoleWithAuthority, qAuthority)
                .from(roleQuery.as(qRole))
                .leftJoin(qRoleAuthority).on(qRole.roleId.eq(qRoleAuthority.roleId))
                .leftJoin(qAuthority).on(qRoleAuthority.authorityId.eq(qAuthority.authorityId))
                .fetch();

        return new Page<>(total, QueryUtils.rowsToTree(
                rows, qRoleWithAuthority, qAuthority,
                RoleWithAuthority::getRoleId,
                Authority::getAuthorityId,
                RoleWithAuthority::getAuthorities,
                RoleWithAuthority::setAuthorities));
    }

    @Override
    public List<Role> getRolesByUser(BigInteger userId) {
        // 用户->角色
        return getQueryFactory().selectDistinct(qRole)
                .from(qRole)
                .join(qUserRole)
                .on(qRole.roleId.eq(qUserRole.roleId))
                .where(qUserRole.userId.eq(userId))
                .fetch();
    }

}
