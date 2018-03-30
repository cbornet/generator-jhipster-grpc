package <%= packageName %>.grpc;

import <%= packageName %>.domain.User;
import <%= packageName %>.service.dto.UserDTO;

import org.mapstruct.*;

import java.util.HashSet;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, uses = {ProtobufMappers.class})
public interface UserProtoMapper {

    @Mapping(target = "firstName", ignore = true)
    @Mapping(target = "lastName", ignore = true)
    <%_ if (databaseType == 'mongodb' || databaseType == 'sql') { _%>
    @Mapping(target = "imageUrl", ignore = true)
    <%_ } _%>
    @Mapping(target = "langKey", ignore = true)
    UserDTO userProtoToUserDTO(UserProto userProto);

    @AfterMapping
    default void userProtoToUserDTO(UserProto userProto, @MappingTarget UserDTO userDTO) {
        userDTO.setFirstName(userProto.getFirstName().isEmpty() ? null : userProto.getFirstName());
        userDTO.setLastName(userProto.getLastName().isEmpty() ? null : userProto.getLastName());
        <%_ if (databaseType == 'mongodb' || databaseType == 'sql') { _%>
        userDTO.setImageUrl(userProto.getImageUrl().isEmpty() ? null : userProto.getImageUrl());
        <%_ } _%>
        userDTO.setLangKey(userProto.getLangKey().isEmpty() ? null : userProto.getLangKey());
        userDTO.setAuthorities(new HashSet<>(userProto.getAuthoritiesList()));
    }

    default UserProto userDTOToUserProto(UserDTO userDTO) {
        if (userDTO == null) {
            return null;
        }
        UserProto.Builder builder = userDTOToUserProtoBuilder(userDTO);
        // Authorities were not mapped because the method name doesn't match javabean setter names.
        if (userDTO.getAuthorities() != null) {
            builder.addAllAuthorities(userDTO.getAuthorities());
        }
        return builder.build();
    }

    default UserProto.Builder createUserProto () {
        return UserProto.newBuilder();
    }

    UserProto.Builder userDTOToUserProtoBuilder (UserDTO userDTO);

    default UserProto userToUserProto(User user) {
        if (user == null) {
            return null;
        }
        return userToUserProtoBuilder(user).build();
    }

    <%_ if (authenticationType !== 'oauth2') { _%>
    @Mapping(target = "password", ignore = true)
    <%_ } _%>
    @Mapping(target = "resetKey", ignore = true)
    UserProto.Builder userToUserProtoBuilder (User user);

    default User userFromId(<% if (databaseType === 'sql' && authenticationType !== 'oauth2') { %>Long<% } else { %>String<% } %> id) {
        if (id == null) {
            return null;
        }
        User user = new User();
        user.setId(id);
        return user;
    }

}
