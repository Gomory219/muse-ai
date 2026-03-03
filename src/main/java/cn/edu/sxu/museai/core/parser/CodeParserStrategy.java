package cn.edu.sxu.museai.core.parser;

public interface CodeParserStrategy<T> {
    T parse(String code);
}
