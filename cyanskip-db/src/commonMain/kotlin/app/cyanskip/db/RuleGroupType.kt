package app.cyanskip.db

object RuleGroupType {
    // Discriminates application and global rule groups in action logs.
    const val App = 2
    const val Global = 3
}
