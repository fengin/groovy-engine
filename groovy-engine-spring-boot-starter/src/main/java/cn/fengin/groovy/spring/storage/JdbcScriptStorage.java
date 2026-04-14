package cn.fengin.groovy.spring.storage;

import cn.fengin.groovy.api.IScriptStorage;
import cn.fengin.groovy.model.GroovyScript;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于 MyBatis-Plus 的 IScriptStorage 默认实现
 * <p>
 * 支持 MySQL、H2 等所有 MyBatis-Plus 兼容的数据库。
 * 业务方可通过自定义 {@link IScriptStorage} Bean 覆盖此实现。
 *
 * @author 凌封 (https://aibook.ren)
 */
public class JdbcScriptStorage implements IScriptStorage {

    private final GroovyScriptMapper mapper;

    public JdbcScriptStorage(GroovyScriptMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public GroovyScript getByBizCode(String bizCode) {
        GroovyScriptEntity entity = mapper.selectOne(
                new LambdaQueryWrapper<GroovyScriptEntity>()
                        .eq(GroovyScriptEntity::getBizCode, bizCode)
                        .eq(GroovyScriptEntity::getStatus, 1));
        return toModel(entity);
    }

    @Override
    public List<GroovyScript> listEnabledScripts() {
        List<GroovyScriptEntity> entities = mapper.selectList(
                new LambdaQueryWrapper<GroovyScriptEntity>()
                        .eq(GroovyScriptEntity::getStatus, 1));
        return entities.stream().map(this::toModel).collect(Collectors.toList());
    }

    @Override
    public GroovyScript getById(Long id) {
        return toModel(mapper.selectById(id));
    }

    @Override
    public List<GroovyScript> listScripts(String category, String projectCode) {
        LambdaQueryWrapper<GroovyScriptEntity> wrapper = new LambdaQueryWrapper<>();
        if (category != null) {
            wrapper.eq(GroovyScriptEntity::getCategory, category);
        }
        if (projectCode != null) {
            wrapper.eq(GroovyScriptEntity::getProjectCode, projectCode);
        }
        // 列表不返回 scriptContent 以节省带宽
        wrapper.select(GroovyScriptEntity::getId, GroovyScriptEntity::getBizCode,
                GroovyScriptEntity::getName, GroovyScriptEntity::getCategory,
                GroovyScriptEntity::getStatus, GroovyScriptEntity::getVersion,
                GroovyScriptEntity::getProjectCode, GroovyScriptEntity::getRemark,
                GroovyScriptEntity::getUpdateTime);
        List<GroovyScriptEntity> entities = mapper.selectList(wrapper);
        return entities.stream().map(this::toModel).collect(Collectors.toList());
    }

    @Override
    public void save(GroovyScript script) {
        GroovyScriptEntity entity = toEntity(script);
        mapper.insert(entity);
        script.setId(entity.getId()); // 回填自增 ID
    }

    @Override
    public void updateById(GroovyScript script) {
        mapper.updateById(toEntity(script));
    }

    @Override
    public void removeById(Long id) {
        mapper.deleteById(id);
    }

    // ==================== Entity ↔ Model 转换 ====================

    private GroovyScript toModel(GroovyScriptEntity entity) {
        if (entity == null) return null;
        GroovyScript model = new GroovyScript();
        BeanUtils.copyProperties(entity, model);
        return model;
    }

    private GroovyScriptEntity toEntity(GroovyScript model) {
        if (model == null) return null;
        GroovyScriptEntity entity = new GroovyScriptEntity();
        BeanUtils.copyProperties(model, entity);
        return entity;
    }
}
