package com.hmall.common.domain;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.common.utils.BeanUtils;
import com.hmall.common.utils.CollUtils;
import com.hmall.common.utils.Convert;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/*
 *  这是一个通用的分页数据传输对象（DTO）类，用于封装分页查询结果
 *
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageDTO<T> {
    protected Long total; // 总记录数
    protected Long pages; // 总页数
    protected List<T> list; // 当前页数据列表

    // 用途：当分页查询没有数据时，返回空对象，而不是 null。
    public static <T> PageDTO<T> empty(Long total, Long pages) {
        return new PageDTO<>(total, pages, CollUtils.emptyList());
    }

    public static <T> PageDTO<T> empty(Page<?> page) {
        return new PageDTO<>(page.getTotal(), page.getPages(), CollUtils.emptyList());
    }

    /// //////////////////////////////////////////////////////////////////

    // 根据 page 构造 PageDTO对象
    public static <T> PageDTO<T> of(Page<T> page) {
        if (page == null) {
            return new PageDTO<>();
        }
        if (CollUtils.isEmpty(page.getRecords())) {
            return empty(page);
        }
        return new PageDTO<>(page.getTotal(), page.getPages(), page.getRecords());
    }

    // 支持 从 R 类型映射到 T 类型。
    //使用 Java 8 Stream 的 map 对每条记录进行转换。
    //适用于前后端 DTO 转换，比如将实体对象 UserEntity 转成 UserDTO。
    public static <T, R> PageDTO<T> of(Page<R> page, Function<R, T> mapper) {
        if (page == null) {
            return new PageDTO<>();
        }
        if (CollUtils.isEmpty(page.getRecords())) {
            return empty(page);
        }
        return new PageDTO<>(page.getTotal(), page.getPages(),
                page.getRecords().stream().map(mapper).collect(Collectors.toList()));
    }

    // 直接传入一个列表【覆盖】 Page 中的记录。
    //用途：查询原分页，但返回的数据经过处理或过滤。
    public static <T> PageDTO<T> of(Page<?> page, List<T> list) {
        return new PageDTO<>(page.getTotal(), page.getPages(), list);
    }


    // 使用类对象类型构造 DTO，用于实体类和传输类的深拷贝转换
    public static <T, R> PageDTO<T> of(Page<R> page, Class<T> clazz) {
        return new PageDTO<>(page.getTotal(), page.getPages(), BeanUtils.copyList(page.getRecords(), clazz));
    }

    // 同上
    public static <T, R> PageDTO<T> of(Page<R> page, Class<T> clazz, Convert<R, T> convert) {
        return new PageDTO<>(page.getTotal(), page.getPages(), BeanUtils.copyList(page.getRecords(), clazz, convert));
    }
}
